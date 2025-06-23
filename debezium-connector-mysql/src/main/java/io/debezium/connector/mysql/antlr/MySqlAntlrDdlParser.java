/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.debezium.connector.mysql.antlr;

import io.debezium.annotation.VisibleForTesting;
import io.debezium.antlr.AntlrDdlParser;
import io.debezium.antlr.AntlrDdlParserListener;
import io.debezium.antlr.DataTypeResolver;
import io.debezium.connector.binlog.charset.BinlogCharsetRegistry;
import io.debezium.connector.binlog.jdbc.BinlogSystemVariables;
import io.debezium.connector.mysql.antlr.listener.MySqlAntlrDdlParserListener;
import io.debezium.ddl.parser.mysql2.generated.MySQLLexer;
import io.debezium.ddl.parser.mysql2.generated.MySQLParser;
import io.debezium.relational.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * An ANTLR based parser for MySQL DDL statements.
 *
 * @author Anisha Mohanty
 */
public class MySqlAntlrDdlParser extends AntlrDdlParser<MySQLLexer, MySQLParser> {

    private final ConcurrentMap<String, String> charsetNameForDatabase = new ConcurrentHashMap<>();
    private final Tables.TableFilter tableFilter;
    private final BinlogCharsetRegistry charsetRegistry;

    @VisibleForTesting
    public MySqlAntlrDdlParser() {
        this(Tables.TableFilter.includeAll());
    }

    @VisibleForTesting
    public MySqlAntlrDdlParser(Tables.TableFilter tableFilter) {
        this(true, false, false, tableFilter, null);
    }

    public MySqlAntlrDdlParser(boolean throwErrorsFromTreeWalk, boolean includeViews, boolean includeComments,
                               Tables.TableFilter tableFilter, BinlogCharsetRegistry charsetRegistry) {
        super(throwErrorsFromTreeWalk, includeViews, includeComments);
        systemVariables = new BinlogSystemVariables();
        this.tableFilter = tableFilter;
        this.charsetRegistry = charsetRegistry;
    }

    @Override
    protected ParseTree parseTree(MySQLParser parser) {
        // to-do
        return null;
    }

    @Override
    protected AntlrDdlParserListener createParseTreeWalkerListener() {
        return new MySqlAntlrDdlParserListener(this);
    }

    @Override
    protected MySQLLexer createNewLexerInstance(CharStream charStreams) {
        return new MySQLLexer(charStreams);
    }

    @Override
    protected MySQLParser createNewParserInstance(CommonTokenStream commonTokenStream) {
        return new MySQLParser(commonTokenStream);
    }

    @Override
    protected boolean isGrammarInUpperCase() {
        return true;
    }

    @Override
    protected DataTypeResolver initializeDataTypeResolver() {
        // to-do
       return null;
    }

    @Override
    protected SystemVariables createNewSystemVariablesInstance() {
        return null;
    }

    /**
     * Provides a map of default character sets by database/schema name.
     *
     * @return map of default character sets.
     */
    public ConcurrentMap<String, String> charsetNameForDatabase() {
        return charsetNameForDatabase;
    }

    /**
     * Parse a name from {@link MySQLParser.IdentifierKeywordContext}.
     *
     * @param identifierKeywordContext identifier keyword context.
     * @return name without quotes.
     */
    public String parseName(MySQLParser.IdentifierKeywordContext identifierKeywordContext) {
        return withoutQuotes(identifierKeywordContext.getText());
    }

    /**
     * Parse qualified table identification from {@link MySQLParser.SimpleIdentifierContext
     * {@link io.debezium.connector.mysql.antlr.legacy.MySqlAntlrDdlParser#currentSchema()} will be used if definition of schema name is not part of the context.
     *
     * @param fullIdContext full id context.
     * @return qualified {@link TableId}.
     */
    public TableId parseQualifiedTableId(MySQLParser.SimpleIdentifierContext simpleIdentifierContext) {
        final char[] fullTableName = simpleIdentifierContext.getText().toCharArray();
        StringBuilder component = new StringBuilder();
        String dbName = null;
        String tableName = null;
        final char EMPTY = '\0';
        char lastQuote = EMPTY;
        for (int i = 0; i < fullTableName.length; i++) {
            char c = fullTableName[i];
            if (isQuote(c)) {
                // Opening quote
                if (lastQuote == EMPTY) {
                    lastQuote = c;
                }
                // Closing quote
                else if (lastQuote == c) {
                    // escape of quote by doubling
                    if (i < fullTableName.length - 1 && fullTableName[i + 1] == c) {
                        component.append(c);
                        i++;
                    }
                    else {
                        lastQuote = EMPTY;
                    }
                }
                // Quote that is part of name
                else {
                    component.append(c);
                }
            }
            // dot that is not in quotes, so name separator
            else if (c == '.' && lastQuote == EMPTY) {
                dbName = component.toString();
                component = new StringBuilder();
            }
            // Any char is part of name including quoted dot
            else {
                component.append(c);
            }
        }
        tableName = component.toString();
        return resolveTableId(dbName != null ? dbName : currentSchema(), tableName);
    }

    /**
     * Parse column names for primary index from {@link MySQLParser.IndexNameContext}. This method will update
     * column to be not optional and set primary key column names to table.
     *
     * @param indexNameContext primary key index column names context.
     * @param tableEditor editor for table where primary key index is parsed.
     */
    public void parsePrimaryIndexColumnNames(MySQLParser.IndexNameContext indexNameContext, TableEditor tableEditor) {

    }

    /**
     * Parse column names for unique index from {@link MySqlParser.IndexColumnNamesContext}. This method will set
     * unique key column names to table if there are no optional.
     *
     * @param indexColumnNamesContext unique key index column names context.
     * @param tableEditor editor for table where primary key index is parsed.
     */
    public void parseUniqueIndexColumnNames(MySqlParser.IndexColumnNamesContext indexColumnNamesContext, TableEditor tableEditor) {
        List<Column> indexColumns = getIndexColumns(indexColumnNamesContext, tableEditor);
        if (indexColumns.stream().filter(col -> Objects.isNull(col) || col.isOptional()).count() > 0) {
            logger.warn("Skip to set unique index columns {} to primary key which including optional columns", indexColumns);
        }
        else {
            tableEditor.setPrimaryKeyNames(indexColumns.stream().map(Column::name).collect(Collectors.toList()));
        }
    }

    /**
     * Determine if a table's unique index should be included when parsing relative unique index statement.
     *
     * @param indexColumnNamesContext unique index column names context.
     * @param tableEditor editor for table where unique index is parsed.
     * @return true if the index is to be included; false otherwise.
     */
    public boolean isTableUniqueIndexIncluded(MySqlParser.IndexColumnNamesContext indexColumnNamesContext, TableEditor tableEditor) {
        return getIndexColumns(indexColumnNamesContext, tableEditor).stream().filter(Objects::isNull).count() == 0;
    }

    private List<Column> getIndexColumns(MySqlParser.IndexColumnNamesContext indexColumnNamesContext, TableEditor tableEditor) {
        return indexColumnNamesContext.indexColumnName().stream()
                .map(indexColumnNameContext -> {
                    String columnName;
                    if (indexColumnNameContext.uid() != null) {
                        columnName = parseName(indexColumnNameContext.uid());
                    }
                    else if (indexColumnNameContext.STRING_LITERAL() != null) {
                        columnName = withoutQuotes(indexColumnNameContext.STRING_LITERAL().getText());
                    }
                    else {
                        columnName = indexColumnNameContext.expression().getText();
                    }
                    return tableEditor.columnWithName(columnName);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get the name of the character set for the current database, via the "character_set_database" system property.
     *
     * @return the name of the character set for the current database, or null if not known ...
     */
    public String currentDatabaseCharset() {
        String charsetName = systemVariables.getVariable(BinlogSystemVariables.CHARSET_NAME_DATABASE);
        if (charsetName == null || "DEFAULT".equalsIgnoreCase(charsetName)) {
            charsetName = systemVariables.getVariable(BinlogSystemVariables.CHARSET_NAME_SERVER);
        }
        return charsetName;
    }

    /**
     * Get the name of the character set for the give table name.
     *
     * @return the name of the character set for the given table, or null if not known ...
     */
    public String charsetForTable(TableId tableId) {
        final String defaultDatabaseCharset = tableId.catalog() != null ? charsetNameForDatabase().get(tableId.catalog()) : null;
        return defaultDatabaseCharset != null ? defaultDatabaseCharset : currentDatabaseCharset();
    }

    /**
     * Runs a function if all given object are not null.
     *
     * @param function function to run; may not be null
     * @param nullableObjects object to be tested, if they are null.
     */
    public void runIfNotNull(Runnable function, Object... nullableObjects) {
        for (Object nullableObject : nullableObjects) {
            if (nullableObject == null) {
                return;
            }
        }
        function.run();
    }

    /**
     * Extracts the enumeration values properly parsed and escaped.
     *
     * @param enumValues the raw enumeration values from the parsed column definition
     * @return the list of options allowed for the {@code ENUM} or {@code SET}; never null.
     */
    public static List<String> extractEnumAndSetOptions(List<String> enumValues) {
        return enumValues.stream()
                .map(io.debezium.connector.mysql.antlr.legacy.MySqlAntlrDdlParser::withoutQuotes)
                .map(io.debezium.connector.mysql.antlr.legacy.MySqlAntlrDdlParser::escapeOption)
                .collect(Collectors.toList());
    }

    public static String escapeOption(String option) {
        // Replace comma to backslash followed by comma (this escape sequence implies comma is part of the option)
        // Replace backlash+single-quote to a single-quote.
        // Replace double single-quote to a single-quote.
        return option.replaceAll(",", "\\\\,").replaceAll("\\\\'", "'").replace("''", "'");
    }

    public Tables.TableFilter getTableFilter() {
        return tableFilter;
    }

    /**
     * Obtains the charset name either form charset if present or from collation.
     *
     * @param charsetNode
     * @param collationNode
     * @return character set
     */
    public String extractCharset(MySQLParser.CharsetNameContext charsetNode, MySQLParser.CollationNameContext collationNode) {
        String charsetName = null;
        if (charsetNode != null && charsetNode.getText() != null) {
            charsetName = withoutQuotes(charsetNode.getText());
        }
        else if (collationNode != null && collationNode.getText() != null) {
            final String collationName = withoutQuotes(collationNode.getText()).toLowerCase();
            for (int index = 0; index < charsetRegistry.getCharsetMapSize(); index++) {
                if (collationName.equals(charsetRegistry.getCollationNameForCollationIndex(index))) {
                    charsetName = charsetRegistry.getCharsetNameForCollationIndex(index);
                    break;
                }
            }
        }
        return charsetName;
    }

    /**
     * Signal an alter table event to ddl changes listener.
     *
     * @param id         the table identifier; may not be null
     * @param previousId the previous name of the view if it was renamed, or null if it was not renamed
     * @param ctx        the start of the statement; may not be null
     */
    public void signalAlterTable(TableId id, TableId previousId, MySQLParser.RenameTableStatementContext ctx) {
        final MySQLParser.RenameTableStatementContext parent = (MySQLParser.RenameTableStatementContext) ctx.getParent();
        Interval interval = new Interval(ctx.getParent().start.getStartIndex(),
                parent.renameTableClause().get(0).start.getStartIndex() - 1);
        String prefix = ctx.getParent().start.getInputStream().getText(interval);
        signalAlterTable(id, previousId, prefix + getText(ctx));
    }
}
