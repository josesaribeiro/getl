package getl.examples.mysql

@BaseScript getl.lang.Getl getl

import groovy.transform.BaseScript

// Oracle database connection (using config content variables)
useMysqlConnection mysqlConnection('demo', true) {
    driverPath = configContent.driverPath
    connectHost = configContent.connectHost
    connectDatabase = configContent.connectDatabase
    schemaName = 'getl_demo'
    login = configContent.login
    password = configContent.password
    sqlHistoryFile = "${configContent.workPath}/mysql.{date}.sql"
}

// Price table
mysqlTable('prices', true) {
    tableName = 'prices'
    field('id') { type = integerFieldType; isKey = true }
    field('name') { type = stringFieldType; isNull = false; length = 50 }
    field('create_date') { type = datetimeFieldType; isNull = false }
    field('price') { type = numericFieldType; isNull = false; length = 9; precision = 2 }
    field('description') { type = textFieldType }
}

// Customers table
mysqlTable('customers', true) { table ->
    tableName = 'customers'
    field('id') { type = integerFieldType; isKey = true }
    field('name') { length = 50 }
    field('customer_type') { length = 10 }
}

// Customer phones table
mysqlTable('customers.phones', true) { table ->
    tableName = 'customer_phones'
    field('customer_id') { type = integerFieldType; isKey = true }
    field('phone') { length = 50; isKey = true }
}

// Sales table
mysqlTable('sales', true) {
    tableName = 'sales'
    field('id') { type = bigintFieldType; isKey = true }
    field('price_id') { type = integerFieldType; isNull = false }
    field('customer_id') { type = integerFieldType; isNull = false }
    field('sale_date') { type = datetimeFieldType; isNull = false }
    field('sale_count') { type = bigintFieldType; isNull = false }
    field('sale_sum') { type = numericFieldType; isNull = false; length = 12; precision = 2 }
}