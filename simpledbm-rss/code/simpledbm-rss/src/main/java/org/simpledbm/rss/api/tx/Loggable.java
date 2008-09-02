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
 *    Email  : d dot majumdar at gmail dot com ignore
 */
package org.simpledbm.rss.api.tx;

import org.simpledbm.rss.api.pm.PageId;
import org.simpledbm.rss.api.st.Storable;
import org.simpledbm.rss.api.wal.Lsn;

/**
 * The Loggable interface and its sub-interfaces define the contract between the Transaction Manager
 * and its clients.
 *  
 * @author Dibyendu Majumdar
 * @since 23-Aug-2005
 * @see org.simpledbm.rss.api.tx.Redoable
 * @see org.simpledbm.rss.api.tx.Undoable
 * @see org.simpledbm.rss.api.tx.Compensation
 * @see org.simpledbm.rss.api.tx.NonTransactionRelatedOperation
 * @see org.simpledbm.rss.api.tx.PostCommitAction
 * @see org.simpledbm.rss.api.tx.ContainerDeleteOperation
 * @see org.simpledbm.rss.api.tx.LoggableFactory
 * @see org.simpledbm.rss.api.tx.BaseLoggable
 */
public interface Loggable extends Storable {

    /**
     * Gets the LSN associated with the log record.
     */
    public Lsn getLsn();

    /**
     * Sets the LSN for the log record.
     */
    public void setLsn(Lsn lsn);

    /**
     * Gets the ID of the TransactionalModule that generated this log record.
     * The TransactionalModule is responsible for redoing or undoing the effects
     * of this log record.
     * @see TransactionalModule
     */
    public int getModuleId();

//    /**
//     * Sets the ID of the TransactionalModule that generated this log record. 
//     * The TransactionalModule is responsible for redoing or undoing the effects
//     * of this log record.
//     */
//    public void setModuleId(int moduleId);

    /**
     * Gets the ID of the Page to which this log is related. Most log records are 
     * related to individual pages. Sometimes
     * a log record may be related to more than one page, for example, a log record that
     * implements {@link MultiPageRedo} interface. Note that Dummy Compensation Log records
     * must return null from this method.
     */
    public PageId getPageId();

    /**
     * Sets the ID of the page for which this log record is being
     * generated. Also sets the type of the page. 
     * When the formatting of a new page is being logged, the page type
     * must be saved so that the correct type of page can be fixed in
     * the buffer pool during recovery.
     */
    public void setPageId(int pageType, PageId pageId);

    /**
     * When the formatting of a new page is being logged, the page type
     * must be saved so that the correct type of page can be fixed in
     * the buffer pool during recovery.
     */
    public int getPageType();

    /**
     * Points to the previous log record generated by the transaction.
     */
    public Lsn getPrevTrxLsn();

    /**
     * Points to the previous log record generated by the transaction.
     */
    public void setPrevTrxLsn(Lsn prevTrxLsn);

    /**
     * Transaction that created the log record. Automatically set when the
     * log record is inserted by the transaction. Note that for log records
     * that are not related to transactions, such as {@link NonTransactionRelatedOperation},
     * the trxid.isNull() will evaluate to true.
     */
    public TransactionId getTrxId();

    /**
     * Transaction that created the log record. Automatically set when the
     * log record is inserted by the transaction.
     */
    public void setTrxId(TransactionId trxId);
}
