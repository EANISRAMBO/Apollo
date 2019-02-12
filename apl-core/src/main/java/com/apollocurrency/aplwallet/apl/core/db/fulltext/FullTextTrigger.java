/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDb;
import org.h2.api.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.enterprise.inject.spi.CDI;

public class FullTextTrigger implements Trigger, TransactionalDb.TransactionCallback {
        private static final Logger LOG = LoggerFactory.getLogger(FullTextTrigger.class);
    /**
     * Pending table updates
     * We collect index row updates and then commit or rollback it when db transaction was finished or rollbacked
     */
    private final List<TableUpdate> tableUpdates = new ArrayList<>();
    /**
     * Trigger cannot have constructor, so these values will be initialized in
     * {@link FullTextTrigger#init(Connection, String, String, String, boolean, int)} method
     */
    private LuceneFullTextSearchEngine ftl;
    private TableData tableData;


    private TableData readTableData(String schema, String tableName) throws SQLException {
        try (Connection con = Db.getDb().getConnection()) {
            return DbUtils.getTableData(con, tableName, schema);
        }
    }

    /**
     * Initialize the trigger (Trigger interface)
     *
     * @param   conn                Database connection
     * @param   schema              Database schema name
     * @param   trigger             Database trigger name
     * @param   table               Database table name
     * @param   before              TRUE if trigger is called before database operation
     * @param   type                Trigger type
     * @throws  SQLException        A SQL error occurred
     */
    @Override
    public void init(Connection conn, String schema, String trigger, String table, boolean before, int type)
            throws SQLException {
        this.tableData = readTableData(schema, table);
        this.ftl = CDI.current().select(LuceneFullTextSearchEngine.class).get();
    }

    /**
     * Close the trigger (Trigger interface)
     *
     * @throws  SQLException        A SQL error occurred
     */
    @Override
    public void close() throws SQLException {}

    /**
     * Remove the trigger (Trigger interface)
     *
     * @throws  SQLException        A SQL error occurred
     */
    @Override
    public void remove() throws SQLException {}

    /**
     * Trigger has fired (Trigger interface)
     *
     * @param   conn                Database connection
     * @param   oldRow              The old row or null
     * @param   newRow              The new row or null
     * @throws  SQLException        A SQL error occurred
     */
    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        //
        // Ignore the trigger if it is not enabled
        //

        //
        // Commit the change immediately if we are not in a transaction
        //
        if (!Db.getDb().isInTransaction()) {
            try {
                ftl.commitRow(oldRow, newRow, tableData);
                ftl.commitIndex();
            } catch (SQLException exc) {
                LOG.error("Unable to update the Lucene index", exc);
            }
            return;
        }
        //
        // Save the table update until the update is committed or rolled back.  Note
        // that the current thread is the application thread performing the update operation.
        //
        synchronized(tableUpdates) {
            tableUpdates.add(new TableUpdate(Thread.currentThread(), oldRow, newRow));
        }
        //
        // Register our transaction callback
        //
        Db.getDb().registerCallback(this);
    }

    /**
     * Commit the table changes for the current transaction (TransactionCallback interface)
     */
    @Override
    public void commit() {
        Thread thread = Thread.currentThread();
        try {
            //
            // Update the Lucene index.  Note that a database transaction is associated
            // with a single thread.  So we will commit just those updates generated
            // by the current thread.
            //
            boolean commit = false;
            synchronized(tableUpdates) {
                Iterator<TableUpdate> updateIt = tableUpdates.iterator();
                while (updateIt.hasNext()) {
                    TableUpdate update = updateIt.next();
                    if (update.getThread() == thread) {
                        ftl.commitRow(update.getOldRow(), update.getNewRow(), tableData);
                        updateIt.remove();
                        commit = true;
                    }
                }
            }
            //
            // Commit the index updates
            //
            if (commit) {
                ftl.commitIndex();
            }
        } catch (SQLException exc) {
            LOG.error("Unable to update the Lucene index", exc);
        }
    }

    /**
     * Discard the table changes for the current transaction (TransactionCallback interface)
     */
    @Override
    public void rollback() {
        Thread thread = Thread.currentThread();
        synchronized(tableUpdates) {
            tableUpdates.removeIf(update -> update.getThread() == thread);
        }
    }
}