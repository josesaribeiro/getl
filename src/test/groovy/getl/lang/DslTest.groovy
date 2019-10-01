package getl.lang

import getl.files.FileManager
import getl.h2.*
import getl.tfs.*
import getl.utils.Config
import getl.utils.DateUtils
import getl.utils.FileUtils
import getl.utils.Logs
import getl.utils.StringUtils
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import groovy.test.GroovyAssert

@FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)
class DslTest extends getl.test.GetlTest {
    /** Temporary path */
    final def tempPath = TFS.systemPath
    /** Config file name */
    final def tempConfig = "$tempPath/getl.conf"
    /** H2 table name */
    final def h2TableName = 'table1'
    /** CSV file name 1 */
    final def csvFileName1 = 'file1.csv'
    /** CSV file name 2 */
    final def csvFileName2 = 'file2.csv'
    /** Count rows in table1 */
    final def table1_rows = 100

    @BeforeClass
    static void CleanGetl() {
        Getl.CleanGetl()
    }

    @Test
    void test01_01SaveFile() {
        Getl.Dsl(this) {
            def file = textFile {
                temporaryFile = true
                write '12345'
            }
            assertEquals('12345', new File(file).text)
        }
    }

    @Test
    void test01_02GenerateAndLoadConfig() {
        Getl.Dsl(this) {
            logInfo "Use temporary path: ${this.tempPath}"

            // Generate configuration file
            def configFileName = textFile(this.tempConfig) {
                temporaryFile = true
                write """
datasets {
    table1 {
        tableName = '${this.h2TableName}'
    }
    
    file1 {
        fileName = '${this.csvFileName1}'
    }
    
    file2 {
        fileName = '${this.csvFileName2}'
    }
}
"""
            }

            assertEquals(this.tempConfig, configFileName)

            // Load configuration
            configuration {
                path = this.tempPath
                load('getl.conf')
            }
        }

        assertEquals(csvFileName1, Config.content.datasets?.file1?.fileName)
        assertEquals(h2TableName, Config.content.datasets?.table1?.tableName)
    }

    @Test
    void test01_03InitLogFile() {
        Getl.Dsl(this) {
            // Init log file
            logging {
                logFileName = "${this.tempPath}/getl.{date}.logs"
                new File(logFileNameHandler).deleteOnExit()
            }
        }

        assertEquals("${tempPath}/getl.{date}.logs", Logs.logFileName)
    }

    @Test
    void test02_01CreateH2Connection() {
        Getl.Dsl(this) {
            // Register connection as H2
            useH2Connection embeddedConnection('getl.testdsl.h2:h2', true) {
                sqlHistoryFile = "${this.tempPath}/getl.lang.h2.sql"
                new File(sqlHistoryFile).deleteOnExit()

                configContent.sqlFileHistoryH2 = sqlHistoryFile
            }
        }

        assertEquals("$tempPath/getl.lang.h2.sql", Getl.Dsl().embeddedConnection('getl.testdsl.h2:h2').sqlHistoryFile)
    }

    @Test
    void test02_02CreateTables() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.h2'

            // Create and generate data to H2 temporary table
            h2Table('table1', true) {
                useConfig 'table1'

                field('id') { type = integerFieldType; isKey = true }
                field('name') { type = stringFieldType; length = 50; isNull = false }
                field('dt') { type = datetimeFieldType; defaultValue = 'Now()'; isNull = false }

                createOpts {
                    hashPrimaryKey = (fieldListKeys.size() > 0)
                    index('idx_1') {
                        ifNotExists = true
                        columns = [fieldByName('dt').name]
                        unique = false
                    }
                }

                assertEquals(this.h2TableName, tableName)

                create()
                assertTrue(exists)
            }

            registerDatasetObject cloneDataset(h2Table('table1')), 'table2', true
            h2Table('table2') {
                tableName = 'table2'
                createOpts {
                    type = isTemporary
                }
                create()
                assertTrue(exists)
            }
        }
    }

    @Test
    void test02_03GenerateDataToTable1() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.h2'

            rowsTo(h2Table('table1')) {
                writeRow { append ->
                    (1..this.table1_rows).each { append id: it, name: "test $it", dt: DateUtils.now }
                }
                assertEquals(this.table1_rows, countRow)
            }
        }
    }

    @Test
    void test02_04DefineFilesFromTablesStructure() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.csv'

            registerConnectionObject csvTempConnection(), 'csv'

            csvTempWithDataset('table1', h2Table('getl.testdsl.h2:table1')) {
                useConfig 'file1'
                readOpts {
                    filter {
                        (it.id > 0 && it.id <= this.table1_rows)
                    }
                }
            }
            assertEquals(h2Table('getl.testdsl.h2:table1').field, csvTemp('table1').field)

            csvTempWithDataset('table2', h2Table('getl.testdsl.h2:table2')) {
                useConfig 'file2'
                readOpts {
                    filter {
                        (it.id > 0 && it.id <= this.table1_rows)
                    }
                }
            }
            assertEquals(h2Table('getl.testdsl.h2:table2').field, csvTemp('table2').field)

            csvTemp('table1') {
                assertEquals(this.csvFileName1, fileName)
            }
            csvTemp('table2') {
                assertEquals(this.csvFileName2, fileName)
            }
        }
    }

    @Test
    void test02_05CopyTable1ToFile1() {
        Getl.Dsl(this) {
            clearGroupFilter()

            copyRows(h2Table('getl.testdsl.h2:table1'), csvTemp('getl.testdsl.csv:table1')) {
                copyRow { t, f ->
                    f.name = StringUtils.ToCamelCase(t.name)
                    f.dt = DateUtils.now
                }
                assertEquals(this.table1_rows, countRow)
            }
        }
    }

    @Test
    void test02_06LoadFile1ToTable1AndTable2() {
        Getl.Dsl(this) { getl ->
            clearGroupFilter()

            rowsToMany([
                    table1: h2Table('getl.testdsl.h2:table1') { truncate() },
                    table2: h2Table('getl.testdsl.h2:table2') { truncate() }
            ]) {
                writeRow { add ->
                    rowProcess(csvTemp('getl.testdsl.csv:table1')) {
                        readRow { row ->
                            row.dt = DateUtils.now
                            add 'table1', row
                            add 'table2', row
                        }
                        assertEquals(this.table1_rows, countRow)
                    }
                }

                assertEquals(this.table1_rows, destinations.table1.updateRows)
                assertEquals(this.table1_rows, destinations.table2.updateRows)
            }
        }
    }

    @Test
    void test02_07SelectQuery() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.h2'

            query('query1', true) {
                query = '''
SELECT
    t1.id as t1_id, t1.name as t1_name, t1.dt as t1_dt,
    t2.id as t2_id, t2.name as t2_name, t2.dt as t2_dt
FROM table1 t1 
    INNER JOIN table2 t2 ON t1.id = t2.id
ORDER BY t1.id'''

                rowProcess {
                    def count = 0
                    count = 0
                    readRow { row ->
                        count++
                        assertEquals(row.t1_id, row.t2_id)
                        assertTrue(row.t1_dt < DateUtils.now)
                        assertEquals(count, row.t1_id)
                    }
                    assertEquals(this.table1_rows, count)
                    assertEquals(countRow, count)
                }
            }
        }
    }

    @Test
    void test02_08ReadTable1WithFilter() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.h2'

            h2Table('table1') {
                readOpts { where = 'id < 3'; order = ['id ASC'] }
                rowProcess {
                    def i = 0
                    readRow { row ->
                        i++
                        assertTrue(row.id < 3);
                        assertTrue(row.t1_dt < DateUtils.now )
                        assertEquals(i, row.id)
                    }
                    assertEquals(2, countRow)
                }
                readOpts { where = null; order = [] }
            }
        }
    }

    @Test
    void test02_09CopyTable1ToTwoFiles() {
        Getl.Dsl(this) {
            clearGroupFilter()

            copyRows(h2Table('getl.testdsl.h2:table1'), csvTemp('getl.testdsl.csv:table1')) {
                childs(csvTemp('getl.testdsl.csv:table2')) {
                    writeRow { add, sourceRow ->
                        sourceRow.name = (sourceRow.name as String).toLowerCase()
                        add sourceRow
                    }
                }

                copyRow { sourceRow, destRow ->
                    destRow.name = (sourceRow.name as String).toUpperCase()
                }

                assertEquals(this.table1_rows, destination.writeRows)
            }
        }
    }

    @Test
    void test02_10CopyFile1ToTwoTables() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.h2'

            h2Table('table1') {
                truncate()
                assertEquals(0, countRow())
            }

            h2Table('table2') {
                truncate()
                assertEquals(0, countRow())
            }

            copyRows(csvTemp('getl.testdsl.csv:table1'), h2Table('table1')) {
                childs(h2Table('table2')) {
                    writeRow { add, sourceRow ->
                        add sourceRow
                    }
                }
            }

            h2Table('table1') {
                assertEquals(this.table1_rows, countRow())
            }

            h2Table('table2') {
                assertEquals(this.table1_rows, countRow())
            }
        }
    }

    @Test
    void test02_11HistoryPoint() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.h2'

            historypoint('history1', true) {
                tableName = 'historytable'
                saveMethod = mergeSave
                create(true)

                assertTrue(exists)
                assertNull(lastValue('table1').value)
                assertNull(lastValue('table2').value)

                saveValue('table1', 1)
                assertEquals(1, lastValue('table1').value)

                def nowDate = DateUtils.now
                saveValue('table2', nowDate)
                assertEquals(nowDate, lastValue('table2').value)

                saveValue('table1', 2)
                assertEquals(2, lastValue('table1').value)

                nowDate = DateUtils.now
                saveValue('table2', nowDate)
                assertEquals(nowDate, lastValue('table2').value)

                clearValue('table1')
                assertNull(lastValue('table1').value)
                assertEquals(nowDate, lastValue('table2').value)

                truncate()
                assertNull(lastValue('table2').value)
            }
        }
    }

    @Test
    void test03_01FileManagers() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.files'

            def fileRootPath = "$systemTempPath/root"
            FileUtils.ValidPath(fileRootPath, true)
            textFile("$fileRootPath/server.txt") {
                temporaryFile = true
                write('Server file')
            }

            def fileLocalPath = "$systemTempPath/local"
            FileUtils.ValidPath(fileLocalPath, true)
            textFile("$fileLocalPath/local.txt") {
                temporaryFile = true
                write('Local file')
            }

            files('files', true) {
                rootPath = fileRootPath
                localDirectory = fileLocalPath

                upload('local.txt')
                removeLocalFile('local.txt')
                assertFalse(FileUtils.ExistsFile("$fileLocalPath/local.txt"))

                buildListFiles {
                    useMaskPath {
                        mask = '{type}.txt'
                    }
                }
                def files1 = fileList.rows(order: ['type', 'filename'])
                assertEquals(2, files1.size())
                assertEquals('local.txt', files1[0].filename)
                assertEquals('server.txt', files1[1].filename)

                downloadListFiles {
                    saveOriginalDate = true
                    deleteLoadedFile = true
                    orderFiles = ['type', 'filename']
                }

                buildListFiles {
                    useMaskPath {
                        mask = '{type}.txt'
                    }
                }
                assertEquals(0, fileList.countRow())
                assertTrue(FileUtils.ExistsFile("$fileLocalPath/local.txt"))
                assertTrue(FileUtils.ExistsFile("$fileLocalPath/server.txt"))

                removeLocalFile('local.txt')
                removeLocalFile('server.txt')
            }
        }
    }

    @Test
    void test04_01ProcessRepositoryObjects() {
        Getl.Dsl(this) {
            clearGroupFilter()

            assertEquals(2, listConnections().size())
            assertEquals(1, listConnections('h*').size())
            assertEquals(1, listConnections('c*').size())
            assertEquals(5, listDatasets().size())
            assertEquals(4, listDatasets('table*').size())
            assertEquals(1, listDatasets('query*').size())
            assertEquals(1, listHistorypoints().size())
            assertEquals(1, listHistorypoints('h*').size())
            assertEquals(1, listFilemanagers().size())
            assertEquals(1, listFilemanagers('f*').size())

            forGroup 'getl.testdsl.h2'
            assertEquals(1, listConnections().size())
            assertEquals(3, listDatasets().size())
            assertEquals(1, listHistorypoints().size())
            assertEquals(0, listFilemanagers().size())

            forGroup 'getl.testdsl.csv'
            assertEquals(1, listConnections().size())
            assertEquals(2, listDatasets().size())
            assertEquals(0, listHistorypoints().size())
            assertEquals(0, listFilemanagers().size())

            forGroup 'getl.testdsl.files'
            assertEquals(0, listConnections().size())
            assertEquals(0, listDatasets().size())
            assertEquals(0, listHistorypoints().size())
            assertEquals(1, listFilemanagers().size())
        }
    }

    @Test
    void test04_02WorkWithPrototype() {
        Getl.Dsl(this) {
            forGroup 'fail-test'
            assertTrue(connection('getl.testdsl.h2:h2') instanceof TDS)
            assertTrue(dataset('getl.testdsl.h2:table1') instanceof H2Table)
            assertEquals(h2Table('getl.testdsl.h2:table2').params, jdbcTable('getl.testdsl.h2:table2').params)
            GroovyAssert.shouldFail { jdbcTable('getl.testdsl.csv:table1') }
            GroovyAssert.shouldFail { dataset('table1') }
            assertTrue(filemanager('getl.testdsl.files:files') instanceof FileManager)
        }
    }

    @Test
    void test04_03LinkDatasets() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.h2'

            def map = linkDatasets(filteringGroup, 'getl.testdsl.csv').sort { a, b -> a.source <=> b.source }
            assertEquals(map[0].source, 'getl.testdsl.h2:table1')
            assertEquals(map[0].destination, 'getl.testdsl.csv:table1')
            assertEquals(map[1].source, 'getl.testdsl.h2:table2')
            assertEquals(map[1].destination, 'getl.testdsl.csv:table2')
        }
    }

    @Test
    void test05_01ThreadConnections() {
        Getl.Dsl(this) {
            def h2Con = embeddedConnection('getl.testdsl.h2:h2')
            def csvCon = csvTempConnection('getl.testdsl.csv:csv')

            thread {
                abortOnError = true
                useList 'getl.testdsl.h2:h2', 'getl.testdsl.csv:csv'
                run { String connectionName ->
                    def con = connection(connectionName)
                    assertTrue(con instanceof TFS || con instanceof TDS)

                    def newcon = connection(connectionName)
                    assertSame(con, newcon)

                    assertFalse(con in [h2Con, csvCon])

                    if (con instanceof TDS)
                        assertEquals(h2Con.params, con.params)
                }
            }
        }
    }

    @Test
    void test05_02ThreadDatasets() {
        Getl.Dsl(this) {
            def h2Table = h2Table('getl.testdsl.h2:table1')
            def csvFile = csvTemp('getl.testdsl.csv:table1') {
                readOpts {
                    onFilter = null
                }
            }
            thread {
                abortOnError = true
                useList(['getl.testdsl.h2:table1', 'getl.testdsl.csv:table1'])
                run { String datasetName ->
                    def ds = dataset(datasetName)
                    assertTrue(ds instanceof TFSDataset || ds instanceof H2Table)

                    def newds = dataset(datasetName)
                    assertSame(ds, newds)
                    assertSame(ds.connection, newds.connection)

                    assertFalse(ds in [h2Table, csvFile])

                    if (ds instanceof H2Table) {
                        assertNotSame(h2Table.connection, ds.connection)
                        assertEquals(h2Table.params, ds.params)
                    }
                    else {
                        assertNotSame(csvFile.connection, ds.connection)
                        assertEquals(csvFile.params, ds.params)
                    }
                }
            }
        }
    }

    @Test
    void test05_03ThreadFilemanagers() {
        Getl.Dsl(this) {
            def fmfiles = filemanager('getl.testdsl.files:files')

            thread {
                abortOnError = true
                useList 'getl.testdsl.files:files'
                run { String filemanagerName ->
                    def fm = filemanager(filemanagerName)
                    assertTrue(fm instanceof FileManager)

                    def newfm = filemanager(filemanagerName)
                    assertSame(fm, newfm)

                    assertNotSame(fmfiles, fm)

                    assertEquals(fmfiles.params, fm.params)
                }
            }
        }
    }

    @Test
    void test05_04ThreadHistoryPoints() {
        Getl.Dsl(this) {
            forGroup 'getl.testdsl.h2'
            def point1 = historypoint('history1')

            thread {
                abortOnError = true
                useList 'history1'
                run { String historyPointName ->
                    def hp = historypoint(historyPointName)

                    def newhp = historypoint(historyPointName)
                    assertSame(hp, newhp)

                    assertNotSame(point1, hp)

                    assertEquals(point1.params, hp.params)
                }
            }
        }
    }

    @Test
    void test05_05CopyDatasets() {
        Getl.Dsl(this) {
            thread {
                abortOnError = true
                useList linkDatasets('getl.testdsl.h2', 'getl.testdsl.csv') {
                    it != 'table2'
                }
                runWithElements {
                    copyRows(h2Table(it.source), csvTemp(it.destination)) {
                        copyRow()
                        assertEquals(source.readRows, destination.writeRows)
                    }
                }
            }
        }
    }

    @Test
    void test99_01UnregisterObjects() {
        Getl.Dsl(this) {
            clearGroupFilter()

            unregisterFileManager'getl.testdsl.files:*'
            GroovyAssert.shouldFail { filemanager('getl.testdsl.files:files') }

            unregisterDataset null, [H2TABLE, EMBEDDEDTABLE]
            GroovyAssert.shouldFail { dataset('getl.testdsl.h2:table1') }
            GroovyAssert.shouldFail { dataset('getl.testdsl.h2:table2') }
            assertEquals(listDatasets().sort(), ['getl.testdsl.csv:table1', 'getl.testdsl.csv:table2', 'getl.testdsl.h2:query1'])
            unregisterDataset()
            GroovyAssert.shouldFail { dataset('getl.testdsl.csv:table1') }
            GroovyAssert.shouldFail { dataset('getl.testdsl.csv:table2') }
            GroovyAssert.shouldFail { dataset('getl.testdsl.h2:query1') }

            unregisterConnection()
            GroovyAssert.shouldFail { embeddedConnection('getl.testdsl.h2:h2') }
        }

//        println new File(Config.content.sqlFileHistoryH2).text
    }

    @Test
    void test99_02RunGetlScript() {
        Getl.Dsl(this) {
            def p1 = 1
            runGroovyClass DslTestScript, {
                param1 = p1
                param2 = p1 + 1
                param3 = 3
                param5 = [1,2,3]
                param6 = [a:1, b:2, c:3]
                paramCountTableRow = this.table1_rows
            }
        }
    }
}