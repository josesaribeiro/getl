package getl.config

import getl.csv.CSVConnection
import getl.h2.H2Connection
import getl.tfs.TFS
import getl.utils.Config
import getl.utils.FileUtils
import getl.utils.MapUtils
import groovy.json.JsonBuilder
import org.junit.Test

/**
 * @author Alexsey Konstantinov
 */
class ConfigFilesTest extends getl.test.GetlTest {
    def h2 = new H2Connection(config: 'h2')
    def csv = new CSVConnection(config: 'csv')

    @Test
    void testSaveLoadConfig() {
        Config.configClassManager = new ConfigFiles()

        def configPath = new TFS()
        def configFile = new File("${configPath.path}/test_config.conf")

        def builder = new JsonBuilder()
        def conf = builder.root {
            map {
                list 'a', 1, null
            }
            var '${test_var}'

            connections {
                h2 {
                    connectURL 'jdbc:h2:tcp://localhost/test'
                    login 'sa'
                    password 'test'
                    connectProperty {
                        db_close_delay "-1"
                    }
                }
                csv {
                    path '.'
                    rowDelimiter '\r\n'
                }
            }
        }
        Config.content.putAll(conf.root)
        Config.SaveConfig(fileName: configFile)
        assertTrue(configFile.exists())
        //        FileUtils.CopyToDir("${configPath.path}/test_config.conf", 'c:/tmp')

        Config.ClearConfig()
        assertTrue(MapUtils.CleanMap(Config.content, ['vars']).isEmpty())

        Config.SetValue('vars.test_var', 'variable value')
        assertEquals(Config.vars.test_var, 'variable value')

        Config.LoadConfig(fileName: configFile)
        assertEquals(Config.content.var, 'variable value')

        assertEquals('jdbc:h2:tcp://localhost/test', h2.connectURL)
        assertEquals('sa', h2.login)
        assertEquals('test', h2.password)
        assertEquals('-1', h2.connectProperty.db_close_delay)

        assertEquals('.', csv.path)
        assertEquals('\r\n', csv.rowDelimiter)

        Config.ClearConfig()
        Config.SetValue('vars.test_var', 'variable value')
        Config.LoadConfig(path: configPath, fileName: 'test_config.conf')
        assertEquals(Config.content.var, 'variable value')
    }
}