/*
 GETL - based package in Groovy, which automates the work of loading and transforming data. His name is an acronym for "Groovy ETL".

 GETL is a set of libraries of pre-built classes and objects that can be used to solve problems unpacking,
 transform and load data into programs written in Groovy, or Java, as well as from any software that supports
 the work with Java classes.
 
 Copyright (C) 2013-2015  Alexsey Konstantonov (ASCRUS)

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License and
 GNU Lesser General Public License along with this program.
 If not, see <http://www.gnu.org/licenses/>.
*/

package getl.jdbc

import getl.jdbc.opts.*
import groovy.transform.InheritConstructors
import getl.cache.*
import getl.exception.ExceptionGETL

/**
 * Table dataset class
 * @author Alexsey Konstantinov
 *
 */
@InheritConstructors
class TableDataset extends JDBCDataset {
	TableDataset() {
		super()
		type = JDBCDataset.Type.TABLE
		sysParams.isTable = true
		methodParams.register("unionDataset", [])
	}

	/**
	 * Table name
	 */
	String getTableName () { params.tableName }
	/**
	 * Table name
	 */
	void setTableName (String value) { params.tableName = value }

	/**
	 * Create table options
	 */
	Map<String, Object> getCreateDirective() { (params.directive as Map<String, Object>).create as Map<String, Object>}
	/**
	 * Create table options
	 */
	void setCreateDirective(Map<String, Object> value) {
		createDirective.clear()
		createDirective.putAll(value)
	}

	/**
	 * Drop table options
	 */
	Map<String, Object> getDropDirective() { (params.directive as Map<String, Object>).drop as Map<String, Object>}
	/**
	 * Drop table options
	 */
	void setDropDirective(Map<String, Object> value) {
		dropDirective.clear()
		dropDirective.putAll(value)
	}

	/**
	 * Read table options
	 */
	Map<String, Object> getReadDirective() { (params.directive as Map<String, Object>).read as Map<String, Object>}
	/**
	 * Read table options
	 */
	void setReadDirective(Map<String, Object> value) {
		readDirective.clear()
		readDirective.putAll(value)
	}

	/**
	 * Write table options
	 */
	Map<String, Object> getWriteDirective() { (params.directive as Map<String, Object>).write as Map<String, Object>}
	/**
	 * Write table options
	 */
	void setWriteDirective(Map<String, Object> value) {
		writeDirective.clear()
		writeDirective.putAll(value)
	}

	/**
	 * Bulk load CSV file options
	 */
	Map<String, Object> getBulkLoadDirective() { (params.directive as Map<String, Object>).bulkLoad as Map<String, Object>}
	/**
	 * Bulk load CSV file options
	 */
	void setBulkLoadDirective(Map<String, Object> value) {
		bulkLoadDirective.clear()
		bulkLoadDirective.putAll(value)
	}

	/**
	 * Read table as update locking
	 */
	Boolean getForUpdate() { params.forUpdate }
	/**
	 * Read table as update locking
	 */
	void setForUpdate(Boolean value) { params.forUpdate = value }

	/**
	 * Read offset row
	 */
	Long getOffs() { params.offs as Long }
	/**
	 * Read offset row
	 */
	void setOffs(Long value) { params.offs = value }

	/**
	 * Read limit row
	 */
	Long getLimit() { params.limit as Long }
	/**
	 * Read limit row
	 */
	void setLimit(Long value) { params.limit = value }

	private CacheManager cacheManager
	/**
	 * Cache manager
	 * Is used to monitor changes in the structure or data
	 */
	CacheManager getCacheManager () { cacheManager }
	/**
	 * Cache manager
	 * Is used to monitor changes in the structure or data
	 */
	void setCacheManager (CacheManager value) {
		if (cacheDataset != null && value != cacheManager) {
			cacheDataset.connection = null
			cacheDataset = null
		}
		
		def isNewCacheManager = (value != null && value != cacheManager)
		
		cacheManager = value
		
		if (isNewCacheManager) {
			cacheDataset = new CacheDataset(connection: cacheManager, dataset: this)
		}
	}

	/**
	 * Cache dataset
	 * Is used to monitor changes in the structure or data
	 */
	private CacheDataset getCacheDataset () { sysParams.cacheDataset as CacheDataset}
	/**
	 * Cache dataset
	 * Is used to monitor changes in the structure or data
	 */
	private void setCacheDataset (CacheDataset value) { sysParams.cacheDataset = value }

	/**
	 * Description table
	 */
	String getDescription () { params.description }
	/**
	 * Description table
	 */
	void setDescription (String value) { params.description = value }
	
	/**
	 * Insert/Update/Delete/Merge records from other dataset
	 */
	long unionDataset (Map procParams) {
		if (procParams == null) procParams = [:]
		methodParams.validation("unionDataset", procParams, [connection.driver.methodParams.params("unionDataset")])
		
		return ((JDBCDriver)connection.driver).unionDataset(this, procParams)
	}
	
	/**
	 * Find key by filter
	 * @param procParams - parameters for query
	 * @return - values of key field or null is not found
	 */
	Map findKey (Map procParams) {
		def keys = getFieldKeys()
		if (keys.isEmpty()) throw new ExceptionGETL("Required key fields")
		procParams = procParams?:[:]
		def r = rows(procParams + [onlyFields: keys, limit: 1])
		if (r.isEmpty()) return null
		
		return r[0]
	}
	
	/**
	 * Return count rows from table
	 */
	long countRow(String where = null, Map procParams = [:]) {
		QueryDataset q = new QueryDataset(connection: connection, query: "SELECT Count(*) AS count FROM ${fullNameDataset()}")
		where = where?:readDirective.where
		if (where != null && where != '') q.query += " WHERE " + where
		def r = q.rows(procParams)
		
		return r[0].count as long
	}
	
	/**
	 * Delete rows for condition
	 */
	long deleteRows (String where = null) {
		String sql = "DELETE FROM ${fullNameDataset()}" + ((where != null && where.trim().length() > 0)?" WHERE $where":'')
		
		long count
		boolean isAutoCommit = !connection.isTran()
		if (isAutoCommit) connection.startTran()
		try {
			count = connection.executeCommand(command: sql, isUpdate: true)
		}
		catch (Throwable e) {
			if (isAutoCommit) connection.rollbackTran()
			throw e
		}
		if (isAutoCommit) connection.commitTran()
		
		return count
	}

	/**
	 * Truncate table
	 */
	void truncate () {
		String sql = "TRUNCATE TABLE ${fullNameDataset()}"
		connection.executeCommand(command: sql, isUpdate: true)
	}

	/**
	 * Full table name
	 */
	String getFullTableName() { fullNameDataset() }

	/**
	 * Create new options object for create table
	 */
	protected CreateSpec newCreateTableParams(Boolean useExternalParams, Map<String, Object> opts) { new CreateSpec(useExternalParams, opts) }

	/**
	 * Generate new options object for create table
	 */
	protected CreateSpec genCreateTable(Closure cl) {
		def parent = newCreateTableParams(true, createDirective)
		if (cl != null) {
			parent.thisObject = parent.DetectClosureDelegate(cl)
			def code = cl.rehydrate(parent.DetectClosureDelegate(cl), parent, parent.DetectClosureDelegate(cl))
			code.resolveStrategy = Closure.OWNER_FIRST
			code.call(this)
			parent.prepareParams()
		}

		return parent
	}

	/**
	 * Create table of specified options
	 */
	CreateSpec createOpts(@DelegatesTo(CreateSpec) Closure cl = null) {
		genCreateTable(cl)
	}

	/**
	 * Create new options object for drop table
	 */
	protected DropSpec newDropTableParams(Boolean useExternalParams, Map<String, Object> opts) { new DropSpec(useExternalParams, opts) }

	/**
	 * Generate new options object for drop table
	 */
	protected DropSpec genDropTable(Closure cl) {
		def parent = newDropTableParams(true, dropDirective)
		if (cl != null) {
			parent.thisObject = parent.DetectClosureDelegate(cl)
			def code = cl.rehydrate(parent.DetectClosureDelegate(cl), parent, parent.DetectClosureDelegate(cl))
			code.resolveStrategy = Closure.OWNER_FIRST
			code.call(this)
			parent.prepareParams()
		}

		return parent
	}

	/**
	 * Drop table
	 */
	DropSpec dropOpts(@DelegatesTo(DropSpec) Closure cl = null) {
		genDropTable(cl)
	}

	/**
	 * Create new options object for reading table
	 */
	protected ReadSpec newReadTableParams(Boolean useExternalParams, Map<String, Object> opts) { new ReadSpec(useExternalParams, opts) }

	/**
	 * Generate new options object for reading table
	 */
	protected ReadSpec genReadDirective(Closure cl) {
		def parent = newReadTableParams(true, readDirective)
		parent.thisObject = parent.DetectClosureDelegate(cl)
		if (cl != null) {
			def code = cl.rehydrate(parent.DetectClosureDelegate(cl), parent, parent.DetectClosureDelegate(cl))
			code.resolveStrategy = Closure.OWNER_FIRST
			code.call(this)
			parent.prepareParams()
		}

		return parent
	}

	/**
	 * Read table options
	 */
	ReadSpec readOpts(@DelegatesTo(ReadSpec) Closure cl = null) {
		genReadDirective(cl)
	}

	/**
	 * Create new options object for writing table
	 */
	protected WriteSpec newWriteTableParams(Boolean useExternalParams, Map<String, Object> opts) { new WriteSpec(useExternalParams, opts) }

	/**
	 * Generate new options object for writing table
	 */
	protected WriteSpec genWriteDirective(Closure cl) {
		def parent = newWriteTableParams(true, writeDirective)
		parent.thisObject = parent.DetectClosureDelegate(cl)
		if (cl != null) {
			def code = cl.rehydrate(parent.DetectClosureDelegate(cl), parent, parent.DetectClosureDelegate(cl))
			code.resolveStrategy = Closure.OWNER_FIRST
			code.call(this)
			parent.prepareParams()
		}

		return parent
	}

	/**
	 * Write table options
	 */
	WriteSpec writeOpts(@DelegatesTo(WriteSpec) Closure cl = null) {
		genWriteDirective(cl)
	}

	/**
	 * Create new options object for writing table
	 */
	protected BulkLoadSpec newBulkLoadTableParams(Boolean useExternalParams, Map<String, Object> opts) { new BulkLoadSpec(useExternalParams, opts) }

	/**
	 * Generate new options object for writing table
	 */
	protected BulkLoadSpec genBulkLoadDirective(Closure cl) {
		def parent = newBulkLoadTableParams(true, bulkLoadDirective)
		parent.thisObject = parent.DetectClosureDelegate(cl)
		if (cl != null) {
			def code = cl.rehydrate(parent.DetectClosureDelegate(cl), parent, parent.DetectClosureDelegate(cl))
			code.resolveStrategy = Closure.OWNER_FIRST
			code.call(this)
			parent.prepareParams()
		}

		return parent
	}

	/**
	 * Write table options
	 */
	BulkLoadSpec bulkLoadOpts(@DelegatesTo(BulkLoadSpec) Closure cl = null) {
		genBulkLoadDirective(cl)
	}
}