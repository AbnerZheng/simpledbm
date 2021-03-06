/***
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *    Project: www.simpledbm.org
 *    Author : Dibyendu Majumdar
 *    Email  : dibyendu@mazumdar.demon.co.uk
 */
package org.simpledbm.samples.tupledemo;

import java.nio.ByteBuffer;
import java.util.Properties;

import org.simpledbm.common.api.key.IndexKey;
import org.simpledbm.common.api.key.IndexKeyFactory;
import org.simpledbm.common.api.platform.Platform;
import org.simpledbm.common.api.platform.PlatformObjects;
import org.simpledbm.common.api.tx.IsolationMode;
import org.simpledbm.common.impl.platform.PlatformImpl;
import org.simpledbm.rss.api.im.IndexContainer;
import org.simpledbm.rss.api.im.IndexScan;
import org.simpledbm.rss.api.loc.Location;
import org.simpledbm.rss.api.tuple.TupleContainer;
import org.simpledbm.rss.api.tuple.TupleInserter;
import org.simpledbm.rss.api.tx.Transaction;
import org.simpledbm.rss.main.Server;
import org.simpledbm.typesystem.api.DictionaryCache;
import org.simpledbm.typesystem.api.Row;
import org.simpledbm.typesystem.api.RowFactory;
import org.simpledbm.typesystem.api.TypeDescriptor;
import org.simpledbm.typesystem.api.TypeFactory;
import org.simpledbm.typesystem.api.TypeSystemFactory;
import org.simpledbm.typesystem.impl.IntegerType;
import org.simpledbm.typesystem.impl.SimpleDictionaryCache;
import org.simpledbm.typesystem.impl.TypeSystemFactoryImpl;
import org.simpledbm.typesystem.impl.VarcharType;

/**
 * A sample database that implements a single table, with two indexes.
 * 
 * @author Dibyendu Majumdar
 * @since 06 May 2007
 */
class TupleDemoDb {

    private Server server;

    private boolean serverStarted = false;

    /** Table container ID */
    public final static int TABLE_CONTNO = 1;
    /** Primary key index container ID */
    public final static int PKEY_CONTNO = 2;
    /** Secondary key index container ID */
    public final static int SKEY1_CONTNO = 3;

    /** Object registry id for row factory */
    final static int ROW_FACTORY_TYPE_ID = 25000;

    Platform platform;

    final DictionaryCache dictionaryCache = new SimpleDictionaryCache();

    TypeSystemFactory typeSystemFactory;

    TypeFactory fieldFactory;

    RowFactory rowFactory;

    Properties properties = getServerProperties();

    public TupleDemoDb() {
        platform = new PlatformImpl(properties);
        PlatformObjects po = platform
                .getPlatformObjects("org.simpledbm.samples.tupledemo");

        typeSystemFactory = new TypeSystemFactoryImpl(properties, po);
        fieldFactory = typeSystemFactory.getDefaultTypeFactory();
        rowFactory = typeSystemFactory.getDefaultRowFactory(fieldFactory,
                dictionaryCache);
    }

    static Properties getServerProperties() {
        Properties properties = new Properties();
        properties.setProperty("log.ctl.1", "ctl.a");
        properties.setProperty("log.ctl.2", "ctl.b");
        properties.setProperty("log.groups.1.path", ".");
        properties.setProperty("log.archive.path", ".");
        properties.setProperty("log.group.files", "3");
        properties.setProperty("log.file.size", "16384");
        properties.setProperty("log.buffer.size", "16384");
        properties.setProperty("log.buffer.limit", "4");
        properties.setProperty("log.flush.interval", "5");
        properties.setProperty("storage.basePath", "demodata/TupleDemo1");
        return properties;
    }

    /**
     * Creates the SimpleDBM server.
     */
    public static void createServer() {
        Server.create(getServerProperties());

        TupleDemoDb server = new TupleDemoDb();
        server.startServer();
        try {
            server.createTableAndIndexes();
        } finally {
            server.shutdownServer();
        }
    }

    /**
     * Starts the SimpleDBM server instance.
     */
    public synchronized void startServer() {
        /*
         * We cannot start the server more than once
         */
        if (serverStarted) {
            throw new RuntimeException("Server is already started");
        }

        /*
         * We must always create a new server object.
         */
        server = new Server(platform, properties);
        registerTableRowType(platform, properties);
        server.start();

        serverStarted = true;
    }

    /**
     * Shuts down the SimpleDBM server instance.
     */
    public synchronized void shutdownServer() {
        if (serverStarted) {
            server.shutdown();
            serverStarted = false;
            server = null;
        }
        platform.shutdown();
    }

    /**
     * Registers a row types for the table, primary key index, and secondary key
     * index.
     * 
     * @param properties
     * @param platform
     */
    void registerTableRowType(Platform platform, Properties properties) {
        /**
         * Table row (id, name, surname, city)
         */
        final TypeDescriptor[] rowtype_for_mytable = new TypeDescriptor[] {
                new IntegerType(), /* primary key */
                new VarcharType(30), /* name */
                new VarcharType(30), /* surname */
                new VarcharType(20) /* city */
        };

        /**
         * Primary key (id)
         */
        final TypeDescriptor[] rowtype_for_pk = new TypeDescriptor[] { rowtype_for_mytable[0] /*
                                                                                              * primary
                                                                                              * key
                                                                                              */
        };

        /**
         * Secondary key (name, surname)
         */
        final TypeDescriptor[] rowtype_for_sk1 = new TypeDescriptor[] {
                rowtype_for_mytable[2], /* surname */
                rowtype_for_mytable[1], /* name */
        };

        dictionaryCache.registerRowType(TABLE_CONTNO, rowtype_for_mytable);
        dictionaryCache.registerRowType(PKEY_CONTNO, rowtype_for_pk);
        dictionaryCache.registerRowType(SKEY1_CONTNO, rowtype_for_sk1);

        server.registerSingleton(ROW_FACTORY_TYPE_ID, rowFactory);
    }

    /**
     * Creates a new row object for the specified container.
     * 
     * @param containerId ID of the container
     * @return Appropriate row type
     */
    Row makeRow(int containerId) {
        RowFactory rowFactory = (RowFactory) server.getObjectRegistry()
                .getSingleton(ROW_FACTORY_TYPE_ID);
        return rowFactory.newRow(containerId);
    }

    /**
     * Creates a new row object for the specified container.
     * 
     * @param containerId ID of the container
     * @return Appropriate row type
     */
    Row makeRow(int containerId, ByteBuffer buf) {
        RowFactory rowFactory = (RowFactory) server.getObjectRegistry()
                .getSingleton(ROW_FACTORY_TYPE_ID);
        return rowFactory.newRow(containerId, buf);
    }

    /**
     * Create a row with values that are less than any other row in the index.
     * 
     * @param containerId ID of the container
     * @return Appropriate row type
     */
    IndexKey makeMinRow(int containerId) {
        IndexKeyFactory rowFactory = (RowFactory) server.getObjectRegistry()
                .getSingleton(ROW_FACTORY_TYPE_ID);
        return rowFactory.minIndexKey(containerId);
    }

    /**
     * Creates the table and associated indexes
     */
    void createTableAndIndexes() {

        Transaction trx = server.begin(IsolationMode.READ_COMMITTED);
        boolean success = false;
        try {
            server.createTupleContainer(trx, "MYTABLE.DAT", TABLE_CONTNO, 8);
            success = true;
        } finally {
            if (success)
                trx.commit();
            else
                trx.abort();
        }

        trx = server.begin(IsolationMode.READ_COMMITTED);
        success = false;
        try {
            server.createIndex(trx, "MYTABLE_PK.IDX", PKEY_CONTNO, 8,
                    ROW_FACTORY_TYPE_ID, true);
            success = true;
        } finally {
            if (success)
                trx.commit();
            else
                trx.abort();
        }

        trx = server.begin(IsolationMode.CURSOR_STABILITY);
        success = false;
        try {
            server.createIndex(trx, "MYTABLE_SKEY1.IDX", SKEY1_CONTNO, 8,
                    ROW_FACTORY_TYPE_ID, false);
            success = true;
        } finally {
            if (success)
                trx.commit();
            else
                trx.abort();
        }
    }

    /**
     * Adds a new row to the table, and updates associated indexes.
     * 
     * @param tableRow Row to be added to the table
     * @throws CloneNotSupportedException
     */
    public void addRow(int id, String name, String surname, String city) {

        Row tableRow = makeRow(TABLE_CONTNO);
        tableRow.setInt(0, id);
        tableRow.setString(1, name);
        tableRow.setString(2, surname);
        tableRow.setString(3, city);

        Row primaryKeyRow = makeRow(PKEY_CONTNO);
        // Set id
        primaryKeyRow.setInt(0, tableRow.getInt(0));

        Row secondaryKeyRow = makeRow(SKEY1_CONTNO);
        // Set surname as the first field
        secondaryKeyRow.setString(0, tableRow.getString(2));
        // Set name
        secondaryKeyRow.setString(1, tableRow.getString(1));

        // Start a new transaction
        Transaction trx = server.begin(IsolationMode.READ_COMMITTED);
        boolean success = false;
        try {
            TupleContainer table = server.getTupleContainer(trx, TABLE_CONTNO);
            IndexContainer primaryIndex = server.getIndex(trx, PKEY_CONTNO);
            IndexContainer secondaryIndex = server.getIndex(trx, SKEY1_CONTNO);

            // First lets create a new row and lock the location
            TupleInserter inserter = table.insert(trx, tableRow);
            // Insert the primary key - may fail with unique constraint
            // violation
            primaryIndex.insert(trx, primaryKeyRow, inserter.getLocation());
            // Insert secondary key
            secondaryIndex.insert(trx, secondaryKeyRow, inserter.getLocation());
            // Complete the insert - may be a no-op.
            inserter.completeInsert();
            success = true;
        } finally {
            if (success) {
                trx.commit();
            } else {
                trx.abort();
            }
        }
    }

    /**
     * Updates an existing row in the table, and its associated indexes.
     */
    public void updateRow(int id, String name, String surname, String city) {

        // Make new row
        Row tableRow = makeRow(TABLE_CONTNO);
        tableRow.setInt(0, id);
        tableRow.setString(1, name);
        tableRow.setString(2, surname);
        tableRow.setString(3, city);

        // New primary key
        Row primaryKeyRow = makeRow(PKEY_CONTNO);
        // Set id
        primaryKeyRow.setInt(0, tableRow.getInt(0));

        // New secondary key
        Row secondaryKeyRow = makeRow(SKEY1_CONTNO);
        // Set surname as the first field
        secondaryKeyRow.setString(0, tableRow.getString(2));
        // Set name
        secondaryKeyRow.setString(1, tableRow.getString(1));

        // Start a new transaction
        Transaction trx = server.begin(IsolationMode.READ_COMMITTED);
        boolean success = false;
        try {
            TupleContainer table = server.getTupleContainer(trx, TABLE_CONTNO);
            IndexContainer primaryIndex = server.getIndex(trx, PKEY_CONTNO);
            IndexContainer secondaryIndex = server.getIndex(trx, SKEY1_CONTNO);

            // Start a scan, with the primary key as argument
            IndexScan indexScan = primaryIndex.openScan(trx, primaryKeyRow,
                    null, true);
            if (indexScan.fetchNext()) {
                // Scan always return item >= search key, so let's
                // check if we had an exact match
                boolean matched = indexScan.getCurrentKey().equals(
                        primaryKeyRow);
                try {
                    if (matched) {
                        // Get location of the tuple
                        Location location = indexScan.getCurrentLocation();
                        // We need the old row data to be able to delete indexes
                        // fetch tuple data
                        byte[] data = table.read(location);
                        // parse the data
                        ByteBuffer bb = ByteBuffer.wrap(data);
                        Row oldTableRow = makeRow(TABLE_CONTNO, bb);
                        // Okay, now update the table row
                        table.update(trx, location, tableRow);
                        // Update secondary indexes
                        // Old secondary key
                        Row oldSecondaryKeyRow = makeRow(SKEY1_CONTNO);
                        // Set surname as the first field
                        oldSecondaryKeyRow.setString(0, oldTableRow
                                .getString(2));
                        // Set name
                        oldSecondaryKeyRow.setString(1, oldTableRow
                                .getString(1));
                        // Delete old key
                        secondaryIndex
                                .delete(trx, oldSecondaryKeyRow, location);
                        // Insert new key
                        secondaryIndex.insert(trx, secondaryKeyRow, location);
                    }
                } finally {
                    //					indexScan.fetchCompleted(matched);
                }
            }
            success = true;
        } finally {
            if (success) {
                trx.commit();
            } else {
                trx.abort();
            }
        }
    }

    /**
     * Updates an existing row in the table, and its associated indexes.
     */
    public void deleteRow(int id) {

        // primary key
        Row primaryKeyRow = makeRow(PKEY_CONTNO);
        // Set id
        primaryKeyRow.setInt(0, id);

        // Start a new transaction
        Transaction trx = server.begin(IsolationMode.READ_COMMITTED);
        boolean success = false;
        try {
            TupleContainer table = server.getTupleContainer(trx, TABLE_CONTNO);
            IndexContainer primaryIndex = server.getIndex(trx, PKEY_CONTNO);
            IndexContainer secondaryIndex = server.getIndex(trx, SKEY1_CONTNO);

            // Start a scan, with the primary key as argument
            IndexScan indexScan = primaryIndex.openScan(trx, primaryKeyRow,
                    null, true);
            if (indexScan.fetchNext()) {
                // Scan always return item >= search key, so let's
                // check if we had an exact match
                boolean matched = indexScan.getCurrentKey().equals(
                        primaryKeyRow);
                try {
                    if (matched) {
                        Location location = indexScan.getCurrentLocation();
                        // We need the old row data to be able to delete indexes
                        // fetch tuple data
                        byte[] data = table.read(location);
                        // parse the data
                        ByteBuffer bb = ByteBuffer.wrap(data);
                        Row oldTableRow = makeRow(TABLE_CONTNO, bb);
                        // Delete tuple data
                        table.delete(trx, location);
                        // Delete secondary index
                        // Make secondary key
                        Row oldSecondaryKeyRow = makeRow(SKEY1_CONTNO);
                        // Set surname as the first field
                        oldSecondaryKeyRow.setString(0, oldTableRow
                                .getString(2));
                        // Set name
                        oldSecondaryKeyRow.setString(1, oldTableRow
                                .getString(1));
                        // Delete old key
                        secondaryIndex
                                .delete(trx, oldSecondaryKeyRow, location);
                        // Delete primary key
                        primaryIndex.delete(trx, primaryKeyRow, location);
                    }
                } finally {
                    //					indexScan.fetchCompleted(matched);
                }
            }
            success = true;
        } finally {
            if (success) {
                trx.commit();
            } else {
                trx.abort();
            }
        }
    }

    /**
     * Prints the contents of a single row.
     * 
     * @param tableRow Row to be printed
     */
    public void printTableRow(Row tableRow) {

        System.out.println("ID = " + tableRow.getString(0) + ", Name = "
                + tableRow.getString(1) + ", Surname = "
                + tableRow.getString(2) + ", City = " + tableRow.getString(3));

    }

    /**
     * Demonstrates how to scan a table using one of the indexes
     * 
     * @param keyContainerId
     */
    public void listRowsByKey(int keyContainerId) {

        Transaction trx = server.begin(IsolationMode.READ_COMMITTED);
        try {
            TupleContainer table = server.getTupleContainer(trx, TABLE_CONTNO);
            IndexContainer index = server.getIndex(trx, keyContainerId);
            IndexScan scan = index.openScan(trx, null, null, false);
            try {
                while (scan.fetchNext()) {
                    Location location = scan.getCurrentLocation();
                    // fetch tuple data
                    byte[] data = table.read(location);
                    // parse the data
                    ByteBuffer bb = ByteBuffer.wrap(data);
                    Row tableRow = makeRow(TABLE_CONTNO, bb);
                    // do something with the row
                    printTableRow(tableRow);
                    // must invoke fetchCompleted
                    //					scan.fetchCompleted(true);
                }
            } finally {
                scan.close();
            }
        } finally {
            trx.abort();
        }
    }

}