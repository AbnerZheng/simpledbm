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
package org.simpledbm.samples.btreedemo;

import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.simpledbm.rss.api.im.Index;
import org.simpledbm.rss.api.im.IndexException;
import org.simpledbm.rss.api.im.IndexScan;
import org.simpledbm.rss.api.loc.Location;
import org.simpledbm.rss.api.loc.LocationFactory;
import org.simpledbm.rss.api.locking.LockDuration;
import org.simpledbm.rss.api.locking.LockMode;
import org.simpledbm.rss.api.tx.IsolationMode;
import org.simpledbm.rss.api.tx.Savepoint;
import org.simpledbm.rss.api.tx.Transaction;
import org.simpledbm.rss.api.tx.TransactionException;
import org.simpledbm.rss.impl.im.btree.BTreeIndexManagerImpl;
import org.simpledbm.rss.main.Server;
import org.simpledbm.rss.util.TypeSize;
import org.simpledbm.rss.util.logging.DiagnosticLogger;
import org.simpledbm.typesystem.api.FieldFactory;
import org.simpledbm.typesystem.api.Row;
import org.simpledbm.typesystem.api.RowFactory;
import org.simpledbm.typesystem.api.TypeDescriptor;
import org.simpledbm.typesystem.impl.DefaultFieldFactory;
import org.simpledbm.typesystem.impl.GenericRowFactory;
import org.simpledbm.typesystem.impl.IntegerType;

/**
 * This class demonstrates how to interface with the BTree module.
 * It builds a small multi-threaded application that allows using two
 * threads for manipulating the same BTree. 
 */
public class BTreeDemo {

	BTreeDatabase db;

	int currentThread = 1;

	Thread t1;

	Thread t2;

	BlockingQueue<Command> commandQueueT1 = new LinkedBlockingQueue<Command>();

	BlockingQueue<Command> commandQueueT2 = new LinkedBlockingQueue<Command>();

	/**
	 * A sample location implementation.
	 */
	public static class RowLocation implements Location {

		int loc;

		public void setInt(int i) {
			this.loc = i;
		}
		
		public void parseString(String string) {
			loc = Integer.parseInt(string);
		}

		public void retrieve(ByteBuffer bb) {
			loc = bb.getInt();
		}

		public void store(ByteBuffer bb) {
			bb.putInt(loc);
		}

		public int getStoredLength() {
			return TypeSize.INTEGER;
		}

		public int compareTo(Location o) {
			RowLocation rl = (RowLocation) o;
			return loc - rl.loc;
		}

		@Override
		public boolean equals(Object o) {
			return compareTo((Location) o) == 0;
		}

		@Override
		public int hashCode() {
			return loc;
		}

		@Override
		public String toString() {
			return "RowLocation(" + loc + ")";
		}

		/**
		 * Unused at present
		 */
		public int getContainerId() {
			return 1;
		}
	}

	/**
	 * Sample location Factory.
	 */
	public static class RowLocationFactory implements LocationFactory {
		public Location newLocation() {
			return new RowLocation();
		}
	}

	public static class BTreeDatabase {

		Server server;
		
		final FieldFactory fieldFactory = new DefaultFieldFactory();

		final RowFactory keyFactory = new GenericRowFactory(fieldFactory);

		final TypeDescriptor[] rowtype1 = new TypeDescriptor[] { new IntegerType() };

		Index btree;

		static int KEY_FACTORY_TYPE = 25000;

		static int LOCATION_FACTORY_TYPE = 25001;

		/**
		 * Encapsulates all the components of the BTree database.
		 * @throws Exception 
		 */
		public BTreeDatabase(boolean create) throws Exception {

			BTreeIndexManagerImpl.setTestingFlag(BTreeIndexManagerImpl.TEST_MODE_LIMIT_MAX_KEYS_PER_PAGE);
			DiagnosticLogger.setDiagnosticsLevel(1);

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
    		properties.setProperty("storage.basePath", "demodata/BTreeDemo");

    		if (create) {
    			Server.create(properties);
    		}

    		server = new Server(properties);
    		server.start();

			keyFactory.registerRowType(1, rowtype1);

			server.getObjectRegistry().register(KEY_FACTORY_TYPE, keyFactory);
			server.getObjectRegistry().register(LOCATION_FACTORY_TYPE, RowLocationFactory.class.getName());

			if (create) {
				createBTree();
			}
		}

		/**
		 * Creates the BTree that will be used for the demo.
		 */
		public void createBTree() throws IndexException, TransactionException {
			Transaction trx = server.getTransactionManager().begin(IsolationMode.CURSOR_STABILITY);
			boolean success = false;
			try {
				server.getIndexManager().createIndex(trx, "testbtree.dat", 1, 8, KEY_FACTORY_TYPE, LOCATION_FACTORY_TYPE, true);
			} finally {
				if (success)
					trx.commit();
				else
					trx.abort();
			}
			// To make it more interesting, lets add 200 keys
			trx = server.getTransactionManager().begin(IsolationMode.CURSOR_STABILITY);
			try {
				Index btree = server.getIndexManager().getIndex(1);
				Row row = (Row) keyFactory.newIndexKey(1);
				LocationFactory locationFactory = (LocationFactory) server.getObjectRegistry().getInstance(LOCATION_FACTORY_TYPE);
				RowLocation location = (RowLocation) locationFactory.newLocation();
				for (int i = 1; i <= 201; i += 2) {
					row.get(0).setInt(i);
					location.setInt(i);
					// Note that the row must be locked exclusively prior to the insert
					trx.acquireLock(location, LockMode.EXCLUSIVE, LockDuration.MANUAL_DURATION);
					btree.insert(trx, row, location);
				}
				success = true;
			} finally {
				if (!success) {
					trx.abort();
				}
				else {
					trx.commit();
				}
			}
		}

		/**
		 * Adds a key/location pair to the tree.
		 */
		public boolean add(Transaction trx, String key, String sloc) throws IndexException, TransactionException {
			boolean success = false;
			Savepoint sp = trx.createSavepoint(false);
			try {
				Index btree = server.getIndexManager().getIndex(1);
				Row row = (Row) keyFactory.newIndexKey(1);
				row.get(0).setString(key);
				LocationFactory locationFactory = (LocationFactory) server.getObjectRegistry().getInstance(LOCATION_FACTORY_TYPE);
				Location location = locationFactory.newLocation();
				location.parseString(sloc);
				// Note that the row must be locked exclusively prior to the insert
				trx.acquireLock(location, LockMode.EXCLUSIVE, LockDuration.MANUAL_DURATION);
				btree.insert(trx, row, location);
				success = true;
			} finally {
				if (!success) {
					trx.rollback(sp);
				}
			}
			return success;
		}

		/**
		 * Adds a key/location pair to the tree.
		 */
		public boolean delete(Transaction trx, String key, String sloc) throws IndexException, TransactionException {
			boolean success = false;
			Savepoint sp = trx.createSavepoint(false);
			try {
				Index btree = server.getIndexManager().getIndex(1);
				Row row = (Row) keyFactory.newIndexKey(1);
				row.get(0).setString(key);
				LocationFactory locationFactory = (LocationFactory) server.getObjectRegistry().getInstance(LOCATION_FACTORY_TYPE);
				Location location = locationFactory.newLocation();
				location.parseString(sloc);
				// Note that the row must be locked exclusively prior to the delete
				trx.acquireLock(location, LockMode.EXCLUSIVE, LockDuration.MANUAL_DURATION);
				btree.delete(trx, row, location);
				success = true;
			} finally {
				if (!success) {
					trx.rollback(sp);
				}
			}
			return success;
		}

		public boolean scan(Transaction trx, String key, String sloc) throws IndexException, TransactionException {
			boolean success = false;
			Savepoint sp = trx.createSavepoint(false);
			try {
				Index btree = server.getIndexManager().getIndex(trx, 1);
				Row row = (Row) keyFactory.newIndexKey(1);
				row.get(0).setString(key);
				LocationFactory locationFactory = (LocationFactory) server.getObjectRegistry().getInstance(LOCATION_FACTORY_TYPE);
				Location location = locationFactory.newLocation();
				location.parseString(sloc);
				IndexScan scan = btree.openScan(trx, row, location, false);
				try {
					while (scan.fetchNext()) {
						DiagnosticLogger.log("SCAN NEXT=" + scan.getCurrentKey() + "," + scan.getCurrentLocation());
						System.err.println("SCAN NEXT=" + scan.getCurrentKey() + "," + scan.getCurrentLocation());
						// trx.releaseLock(scan.getCurrentLocation());
						scan.fetchCompleted(true);
					}
				} finally {
					if (scan != null) {
						scan.close();
					}
				}
				success = true;
			} finally {
				if (!success) {
					trx.rollback(sp);
				}
			}
			return success;
		}
		
		
		/**
		 * Shuts down the database components.
		 */
		public void shutdown() {
			System.err.println("Shutting down");
			server.shutdown();
		}
	}

	/**
	 * Posts a command to the currently active thread.
	 */
	void postCommand(Command cmd) {
		try {
			if (currentThread == 1) {
				commandQueueT1.put(cmd);
			} else {
				commandQueueT2.put(cmd);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Processes the specified command.
	 */
	void processCommand(String thr, String cmd, String arg1, String arg2) {

		System.err.println("Thread=[" + thr + "], Command=[" + cmd + "], arg1=[" + arg1 + "], arg2=[" + arg2 + "]");
		if (thr.equalsIgnoreCase("T1")) {
			currentThread = 1;
		} else {
			currentThread = 2;
		}
		if (cmd.equalsIgnoreCase("Add") || cmd.equalsIgnoreCase("Delete") || cmd.equalsIgnoreCase("List")) {
			postCommand(new Command(cmd, arg1, arg2));
		} else if (cmd.equalsIgnoreCase("Commit") || cmd.equalsIgnoreCase("Abort")) {
			postCommand(new Command(cmd, null, null));
		} else {
			System.err.println("Invalid command " + cmd);
		}
	}

	public void startThreads(DiagnosticLogger.LogHandler os1, DiagnosticLogger.LogHandler os2) {
		t1 = new Thread(new CommandProcessor(os1, db, commandQueueT1), "T1");
		t2 = new Thread(new CommandProcessor(os2, db, commandQueueT2), "T2");

		t1.start();
		t2.start();
	}

	public void stopThreads() {
		System.err.println("Stopping threads");
		try {
			commandQueueT1.put(new Command("Quit", null, null));
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			commandQueueT2.put(new Command("Quit", null, null));
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		try {
			t1.join(10 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			t2.join(10 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean openDatabase() {
		if (db != null) {
			return false;
		}
		try {
			db = new BTreeDatabase(false);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean createDatabase() {
		if (db != null) {
			return false;
		}
		try {
			db = new BTreeDatabase(true);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void shutdown() {
		stopThreads();
		if (db != null) {
			db.shutdown();
		}
	}

	public static class Command {
		private final String cmd;

		private final String arg1;

		private final String arg2;

		final String getArg1() {
			return arg1;
		}

		public Command(String cmd, String arg, String arg2) {
			this.cmd = cmd;
			this.arg1 = arg;
			this.arg2 = arg2;
		}

		final String getCommand() {
			return cmd;
		}

		final String getArg2() {
			return arg2;
		}
	}

	public static class CommandProcessor implements Runnable {

		BlockingQueue<Command> commandQueue;

		BTreeDatabase db;

		Transaction trx = null;

		boolean errors = false;

		DiagnosticLogger.LogHandler os;

		void startTransaction() {
			if (trx == null) {
				DiagnosticLogger.log("Starting new transaction");
				trx = db.server.getTransactionManager().begin(IsolationMode.CURSOR_STABILITY);
				errors = false;
			}
		}

		void add(String key, String location) {
			try {
				startTransaction();
				db.add(trx, key, location);
			} catch (Exception e) {
				errors = true;
				DiagnosticLogger.log("Error occurred: " + e.getMessage());
				e.printStackTrace();
			}
		}

		void delete(String key, String location) {
			try {
				startTransaction();
				db.delete(trx, key, location);
			} catch (Exception e) {
				errors = true;
				DiagnosticLogger.log("Error occurred: " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		void scan(String key, String location) {
			try {
				startTransaction();
				db.scan(trx, key, location);
			} catch (Exception e) {
				errors = true;
				DiagnosticLogger.log("Error occurred: " + e.getMessage());
				e.printStackTrace();
			}
		}

		void commit() {
			try {
				if (trx != null) {
					trx.commit();
					trx = null;
				} else {
					DiagnosticLogger.log("No active transaction");
				}
			} catch (Exception e) {
				DiagnosticLogger.log("Error occurred: " + e.getMessage());
			} finally {
				if (trx != null) {
					errors = true;
					abort();
				}
			}
		}

		void abort() {
			if (trx != null) {
				try {
					trx.abort();
				} catch (TransactionException e) {
					DiagnosticLogger.log("Error occurred: " + e.getMessage());
				}
				trx = null;
			} else {
				DiagnosticLogger.log("No active transaction");
			}
		}

		public CommandProcessor(DiagnosticLogger.LogHandler os, BTreeDatabase db, BlockingQueue<Command> commandQueue) {
			this.os = os;
			this.db = db;
			this.commandQueue = commandQueue;
		}

		public void run() {

			DiagnosticLogger.setHandler(os);
			DiagnosticLogger.log("Thread started");
			for (;;) {
				Command request = null;
				try {
					request = commandQueue.take();
				} catch (InterruptedException e) {
					DiagnosticLogger.log("Error occurred: " + e.getMessage());
				}
				if (request.getCommand().equalsIgnoreCase("quit")) {
					DiagnosticLogger.log("Thread terminating");
					break;
				} else if (request.getCommand().equalsIgnoreCase("add")) {
					DiagnosticLogger.log("Performing add");
					add(request.getArg1(), request.getArg2());
				} else if (request.getCommand().equalsIgnoreCase("delete")) {
					DiagnosticLogger.log("Performing delete");
					delete(request.getArg1(), request.getArg2());
				} else if (request.getCommand().equalsIgnoreCase("list")) {
					DiagnosticLogger.log("Performing list");
					scan(request.getArg1(), request.getArg2());
				} else if (request.getCommand().equalsIgnoreCase("commit")) {
					DiagnosticLogger.log("Performing commit");
					commit();
				} else if (request.getCommand().equalsIgnoreCase("abort")) {
					DiagnosticLogger.log("Performing abort");
					abort();
				}
			}

			abort();
		}
	}

}
