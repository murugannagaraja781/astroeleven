require('dotenv').config();
const mysql = require('mysql2/promise');

// Setup connection pool using env parameters
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 15,
    queueLimit: 0,
    enableKeepAlive: true,
    keepAliveInitialDelay: 10000
});

// Pluralize/snake_case table helper
function getTableName(modelName) {
    const s = modelName.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase();
    if (s.endsWith('y')) {
        return s.slice(0, -1) + 'ies';
    }
    if (s.endsWith('s') || s.endsWith('x')) {
        return s + 'es';
    }
    return s + 's';
}

function getFieldSqlType(key, type) {
    let typeStr = 'TEXT';
    
    // Handle full config objects: { type: String, default: 'x' }
    if (type && typeof type === 'object' && type.type) {
        type = type.type;
    }

    if (type === String) {
        // Optimize common key identifiers to VARCHAR for index capability
        if (['userId', 'phone', 'email', 'sessionId', 'paymentId', 'requestId', 'astrologerId', 'clientId', 'senderId', 'receiverId', 'role', 'status', 'approvalStatus'].includes(key)) {
            typeStr = 'VARCHAR(255)';
        } else {
            typeStr = 'TEXT';
        }
    } else if (type === Number) {
        typeStr = 'DOUBLE';
    } else if (type === Boolean) {
        typeStr = 'TINYINT(1) DEFAULT 0';
    } else if (type === Date) {
        typeStr = 'DATETIME';
    }
    return typeStr;
}

// Convert Mongoose Schema field definition to SQL columns
function schemaToSqlColumns(schemaDefinition) {
    const columns = [];
    columns.push('id INT AUTO_INCREMENT PRIMARY KEY');

    for (const key in schemaDefinition) {
        if (key === 'id' || key === '_id') continue;
        const typeStr = getFieldSqlType(key, schemaDefinition[key]);
        columns.push(`\`${key}\` ${typeStr}`);
    }
    return columns.join(', ');
}

// Helper to escape SQL values and generate placeholders
function buildWhereClause(query) {
    if (!query || Object.keys(query).length === 0) {
        return { clause: '1=1', values: [] };
    }

    const clauses = [];
    const values = [];

    for (const key in query) {
        let val = query[key];

        // Map MongoDB _id to SQL auto-increment id
        const dbKey = (key === '_id') ? 'id' : key;

        // Support for $or operator
        if (key === '$or' && Array.isArray(val)) {
            const orClauses = [];
            for (const subQuery of val) {
                const { clause: subClause, values: subValues } = buildWhereClause(subQuery);
                orClauses.push(`(${subClause})`);
                values.push(...subValues);
            }
            if (orClauses.length > 0) {
                clauses.push(`(${orClauses.join(' OR ')})`);
            }
            continue;
        }

        if (val instanceof RegExp) {
            clauses.push(`\`${dbKey}\` LIKE ?`);
            let pattern = val.source.replace(/^\^|\$$/g, '');
            values.push(`%${pattern}%`);
        } else if (val && typeof val === 'object' && !Array.isArray(val) && !(val instanceof Date)) {
            // Parse operators: $ne, $gte, $lte, $in, $gt, $lt, $exists
            const operators = Object.keys(val);
            for (const op of operators) {
                const opVal = val[op];
                if (op === '$ne') {
                    if (opVal === null) {
                        clauses.push(`\`${dbKey}\` IS NOT NULL`);
                    } else {
                        clauses.push(`(\`${dbKey}\` IS NULL OR \`${dbKey}\` != ?)`);
                        values.push(opVal);
                    }
                } else if (op === '$gte') {
                    clauses.push(`\`${dbKey}\` >= ?`);
                    values.push(opVal);
                } else if (op === '$lte') {
                    clauses.push(`\`${dbKey}\` <= ?`);
                    values.push(opVal);
                } else if (op === '$gt') {
                    clauses.push(`\`${dbKey}\` > ?`);
                    values.push(opVal);
                } else if (op === '$lt') {
                    clauses.push(`\`${dbKey}\` < ?`);
                    values.push(opVal);
                } else if (op === '$in') {
                    if (Array.isArray(opVal) && opVal.length > 0) {
                        const placeholders = opVal.map(() => '?').join(', ');
                        clauses.push(`\`${dbKey}\` IN (${placeholders})`);
                        values.push(...opVal);
                    } else {
                        clauses.push('1=0'); // Match nothing if empty array
                    }
                } else if (op === '$exists') {
                    if (opVal === true || opVal === 1 || opVal === 'true') {
                        clauses.push(`\`${dbKey}\` IS NOT NULL`);
                    } else {
                        clauses.push(`\`${dbKey}\` IS NULL`);
                    }
                } else if (op === '$regex') {
                    clauses.push(`\`${dbKey}\` LIKE ?`);
                    let pattern = '';
                    if (opVal instanceof RegExp) {
                        pattern = opVal.source;
                    } else {
                        pattern = String(opVal);
                    }
                    pattern = pattern.replace(/^\^|\$$/g, '');
                    values.push(`%${pattern}%`);
                }
            }
        } else {
            if (val === null) {
                clauses.push(`\`${dbKey}\` IS NULL`);
            } else {
                clauses.push(`\`${dbKey}\` = ?`);
                values.push(val);
            }
        }
    }

    return {
        clause: clauses.length > 0 ? clauses.join(' AND ') : '1=1',
        values
    };
}

// Chainable Query Builder class mimicking Mongoose
class QueryChain {
    constructor(model, baseSql, baseValues, isFindOne = false) {
        this.model = model;
        this.baseSql = baseSql;
        this.baseValues = baseValues;
        this.isFindOne = isFindOne;
        this._sort = '';
        this._limit = '';
        this._offset = '';
    }

    sort(orderObj) {
        if (!orderObj) return this;
        const keys = Object.keys(orderObj);
        if (keys.length > 0) {
            const key = keys[0];
            const dir = (orderObj[key] === -1 || orderObj[key] === 'desc') ? 'DESC' : 'ASC';
            const dbKey = (key === '_id') ? 'id' : key;
            this._sort = ` ORDER BY \`${dbKey}\` ${dir}`;
        }
        return this;
    }

    limit(n) {
        if (n !== undefined) {
            this._limit = ` LIMIT ${Number(n)}`;
        }
        return this;
    }

    skip(n) {
        if (n !== undefined) {
            this._offset = ` OFFSET ${Number(n)}`;
        }
        return this;
    }

    select(fields) {
        // Suppress or ignore field selection in SQL to return full objects
        return this;
    }

    lean() {
        // Mongoose lean returns plain JS objects, our WrappedDocument is fully compatible
        return this;
    }

    exec() {
        // Returns the QueryChain itself which acts as a Promise
        return this;
    }

    // Support for Promise then/catch to work with async/await
    async then(onfulfilled, onrejected) {
        try {
            let finalSql = this.baseSql + this._sort;
            if (this.isFindOne && !this._limit) {
                finalSql += ' LIMIT 1';
            } else if (this._limit) {
                finalSql += this._limit;
                if (this._offset) {
                    finalSql += this._offset;
                }
            } else if (this._offset) {
                finalSql += ` LIMIT 18446744073709551615${this._offset}`;
            }

            const [rows] = await pool.execute(finalSql, this.baseValues);
            
            if (this.isFindOne) {
                const doc = rows.length > 0 ? this.model.wrap(rows[0]) : null;
                return onfulfilled ? onfulfilled(doc) : doc;
            } else {
                const docs = rows.map(r => this.model.wrap(r));
                return onfulfilled ? onfulfilled(docs) : docs;
            }
        } catch (err) {
            if (onrejected) return onrejected(err);
            throw err;
        }
    }

    async catch(onrejected) {
        return this.then(null, onrejected);
    }
}

// Mongoose document instance wrapper class
class WrappedDocument {
    constructor(model, rawData) {
        this.model = model;
        
        // Copy id and _id if present in rawData
        if (rawData.id !== undefined) this.id = rawData.id;
        if (rawData._id !== undefined) this._id = rawData._id;

        // Deserialize JSON columns and apply defaults based on schema definition
        const definition = model.schema.definition;
        for (const key in definition) {
            let val = rawData[key];
            const fieldDef = definition[key];
            const isBoolType = fieldDef === Boolean || (fieldDef && typeof fieldDef === 'object' && fieldDef.type === Boolean);

            if (typeof val === 'string' && (val.startsWith('{') || val.startsWith('['))) {
                try {
                    val = JSON.parse(val);
                } catch (e) {
                    // Fail-safe: keep string if not parseable JSON
                }
            }

            // Apply default value if null or undefined
            if (val === undefined || val === null) {
                if (Array.isArray(fieldDef)) {
                    val = [];
                } else if (fieldDef && typeof fieldDef === 'object' && fieldDef.default !== undefined) {
                    val = (typeof fieldDef.default === 'function') ? fieldDef.default() : fieldDef.default;
                } else {
                    val = isBoolType ? false : null;
                }
            }

            // Cast to boolean if field type is Boolean in schema
            if (isBoolType) {
                val = (val === 1 || val === '1' || val === true || val === 'true');
            }

            this[key] = val;
        }

        // Mongoose _id compatibility
        if (this.id !== undefined && this._id === undefined) {
            this._id = this.id;
        }

        // Keep raw internal object representation
        this._doc = this;
    }

    toObject() {
        const obj = { ...this };
        delete obj.model;
        delete obj._doc;
        return obj;
    }

    toJSON() {
        return this.toObject();
    }

    async save() {
        const rawData = this.toObject();
        const docId = rawData.id;
        delete rawData.id;
        delete rawData._id;

        // Serialize arrays/objects to JSON strings
        for (const key in rawData) {
            let val = rawData[key];
            if (val && typeof val === 'object' && !(val instanceof Date)) {
                rawData[key] = JSON.stringify(val);
            } else if (val === undefined) {
                rawData[key] = null;
            }
        }

        // Filter and map fields to only columns that exist in the MySQL table
        let dataToSave = {};
        const allowedColumns = this.model.columnNames;
        if (allowedColumns && allowedColumns.length > 0) {
            const allowedLower = allowedColumns.map(c => c.toLowerCase());
            for (const key in rawData) {
                if (allowedLower.includes(key.toLowerCase())) {
                    const dbColName = allowedColumns.find(c => c.toLowerCase() === key.toLowerCase()) || key;
                    dataToSave[dbColName] = rawData[key];
                }
            }
        } else {
            dataToSave = rawData;
        }

        const keys = Object.keys(dataToSave);

        if (docId) {
            // Update
            const setClause = keys.map(k => `\`${k}\` = ?`).join(', ');
            const values = keys.map(k => dataToSave[k]);
            values.push(docId);

            const sql = `UPDATE \`${this.model.tableName}\` SET ${setClause} WHERE id = ?`;
            await pool.execute(sql, values);
        } else {
            // Insert
            const cols = keys.map(k => `\`${k}\``).join(', ');
            const placeholders = keys.map(() => '?').join(', ');
            const values = keys.map(k => dataToSave[k]);

            const sql = `INSERT INTO \`${this.model.tableName}\` (${cols}) VALUES (${placeholders})`;
            const [result] = await pool.execute(sql, values);
            this.id = result.insertId;
            this._id = result.insertId;
        }
        return this;
    }

    async updateOne(updateObj) {
        return this.model.updateOne({ id: this.id }, updateObj);
    }

    async deleteOne() {
        return this.model.deleteOne({ id: this.id });
    }
}

// Custom Model implementation
class CustomModel {
    constructor(name, schema) {
        this.name = name;
        this.schema = schema;
        this.tableName = getTableName(name);
        this.columnNames = [];
        
        // Auto-create database tables on startup
        this.initTable().catch(err => {
            console.error(`❌ Table creation error on ${this.tableName}:`, err.message);
        });
    }

    async initTable() {
        const columnsSql = schemaToSqlColumns(this.schema.definition);
        const sql = `CREATE TABLE IF NOT EXISTS \`${this.tableName}\` (${columnsSql}) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;`;
        await pool.execute(sql);

        // Fetch existing columns to dynamically run ALTER TABLE migrations for missing columns
        const [existingCols] = await pool.execute(`SHOW COLUMNS FROM \`${this.tableName}\``);
        const existingColNames = existingCols.map(c => c.Field.toLowerCase());
        this.columnNames = existingCols.map(c => c.Field);

        for (const key in this.schema.definition) {
            if (key === 'id' || key === '_id') continue;
            if (!existingColNames.includes(key.toLowerCase())) {
                const typeStr = getFieldSqlType(key, this.schema.definition[key]);
                const alterSql = `ALTER TABLE \`${this.tableName}\` ADD COLUMN \`${key}\` ${typeStr}`;
                await pool.execute(alterSql);
                console.log(`📡 Auto-migration: Added column \`${key}\` to table \`${this.tableName}\``);
                this.columnNames.push(key);
            }
        }

        console.log(`📡 Auto-migration: Checked/Created table \`${this.tableName}\``);
    }

    wrap(row) {
        if (!row) return null;
        return new WrappedDocument(this, row);
    }

    find(query = {}) {
        const { clause, values } = buildWhereClause(query);
        const sql = `SELECT * FROM \`${this.tableName}\` WHERE ${clause}`;
        return new QueryChain(this, sql, values, false);
    }

    findOne(query = {}) {
        const { clause, values } = buildWhereClause(query);
        const sql = `SELECT * FROM \`${this.tableName}\` WHERE ${clause}`;
        return new QueryChain(this, sql, values, true);
    }

    findById(id) {
        const sql = `SELECT * FROM \`${this.tableName}\` WHERE id = ?`;
        return new QueryChain(this, sql, [id], true);
    }

    async create(data) {
        const doc = new WrappedDocument(this, data);
        await doc.save();
        return doc;
    }

    deleteOne(query) {
        const promise = (async () => {
            const { clause, values } = buildWhereClause(query);
            const sql = `DELETE FROM \`${this.tableName}\` WHERE ${clause} LIMIT 1`;
            const [result] = await pool.execute(sql, values);
            return { deletedCount: result.affectedRows };
        })();
        promise.exec = () => promise;
        return promise;
    }

    deleteMany(query) {
        const promise = (async () => {
            const { clause, values } = buildWhereClause(query);
            const sql = `DELETE FROM \`${this.tableName}\` WHERE ${clause}`;
            const [result] = await pool.execute(sql, values);
            return { deletedCount: result.affectedRows };
        })();
        promise.exec = () => promise;
        return promise;
    }

    updateOne(query, updateObj) {
        const promise = (async () => {
            const { clause, values } = buildWhereClause(query);
            
            let setFields = {};
            if (updateObj.$set) {
                setFields = updateObj.$set;
            } else if (!updateObj.startsWith && !updateObj.$unset && !updateObj.$inc) {
                setFields = updateObj;
            }

            // Handle numeric increments ($inc)
            if (updateObj.$inc) {
                const incFields = [];
                const incValues = [];
                for (const key in updateObj.$inc) {
                    incFields.push(`\`${key}\` = IFNULL(\`${key}\`, 0) + ?`);
                    incValues.push(updateObj.$inc[key]);
                }
                const sql = `UPDATE \`${this.tableName}\` SET ${incFields.join(', ')} WHERE ${clause} LIMIT 1`;
                const [result] = await pool.execute(sql, [...incValues, ...values]);
                return { matchedCount: result.affectedRows, modifiedCount: result.affectedRows };
            }

            // Handle field deletions ($unset)
            if (updateObj.$unset) {
                const unsetFields = [];
                for (const key in updateObj.$unset) {
                    unsetFields.push(`\`${key}\` = NULL`);
                }
                const sql = `UPDATE \`${this.tableName}\` SET ${unsetFields.join(', ')} WHERE ${clause} LIMIT 1`;
                const [result] = await pool.execute(sql, values);
                return { matchedCount: result.affectedRows, modifiedCount: result.affectedRows };
            }

            const keys = Object.keys(setFields);
            if (keys.length === 0) return { matchedCount: 0, modifiedCount: 0 };

            const setClause = keys.map(k => `\`${k}\` = ?`).join(', ');
            const updateValues = keys.map(k => {
                let val = setFields[k];
                if (val && typeof val === 'object' && !(val instanceof Date)) {
                    return JSON.stringify(val);
                }
                return val === undefined ? null : val;
            });

            const sql = `UPDATE \`${this.tableName}\` SET ${setClause} WHERE ${clause} LIMIT 1`;
            const [result] = await pool.execute(sql, [...updateValues, ...values]);
            return { matchedCount: result.affectedRows, modifiedCount: result.affectedRows };
        })();
        promise.exec = () => promise;
        return promise;
    }

    updateMany(query, updateObj) {
        const promise = (async () => {
            const { clause, values } = buildWhereClause(query);
            
            let setFields = {};
            if (updateObj.$set) {
                setFields = updateObj.$set;
            } else if (!updateObj.$unset && !updateObj.$inc) {
                setFields = updateObj;
            }

            if (updateObj.$inc) {
                const incFields = [];
                const incValues = [];
                for (const key in updateObj.$inc) {
                    incFields.push(`\`${key}\` = IFNULL(\`${key}\`, 0) + ?`);
                    incValues.push(updateObj.$inc[key]);
                }
                const sql = `UPDATE \`${this.tableName}\` SET ${incFields.join(', ')} WHERE ${clause}`;
                const [result] = await pool.execute(sql, [...incValues, ...values]);
                return { matchedCount: result.affectedRows, modifiedCount: result.affectedRows };
            }

            const keys = Object.keys(setFields);
            if (keys.length === 0) return { matchedCount: 0, modifiedCount: 0 };

            const setClause = keys.map(k => `\`${k}\` = ?`).join(', ');
            const updateValues = keys.map(k => {
                let val = setFields[k];
                if (val && typeof val === 'object' && !(val instanceof Date)) {
                    return JSON.stringify(val);
                }
                return val === undefined ? null : val;
            });

            const sql = `UPDATE \`${this.tableName}\` SET ${setClause} WHERE ${clause}`;
            const [result] = await pool.execute(sql, [...updateValues, ...values]);
            return { matchedCount: result.affectedRows, modifiedCount: result.affectedRows };
        })();
        promise.exec = () => promise;
        return promise;
    }

    findByIdAndDelete(id) {
        const promise = (async () => {
            const sql = `DELETE FROM \`${this.tableName}\` WHERE id = ?`;
            const [result] = await pool.execute(sql, [id]);
            return result.affectedRows > 0 ? { id } : null;
        })();
        promise.exec = () => promise;
        return promise;
    }

    findByIdAndUpdate(id, update, options = {}) {
        return this.findOneAndUpdate({ id }, update, options);
    }

    findOneAndUpdate(query, update, options = {}) {
        const promise = (async () => {
            const doc = await this.findOne(query);
            if (!doc) return null;
            await this.updateOne(query, update);
            if (options.new || options.returnDocument === 'after') {
                return await this.findOne(query);
            }
            return doc;
        })();
        promise.exec = () => promise;
        return promise;
    }

    async countDocuments(query = {}) {
        const { clause, values } = buildWhereClause(query);
        const sql = `SELECT COUNT(*) AS count FROM \`${this.tableName}\` WHERE ${clause}`;
        const [rows] = await pool.execute(sql, values);
        return rows[0].count;
    }

    async aggregate(pipeline) {
        if (this.modelName === 'BillingLedger') {
            const sql = `
                SELECT 
                    IFNULL(SUM(chargedToClient), 0) AS usageRevenue,
                    IFNULL(SUM(creditedToAstrologer), 0) AS totalAstroPayout,
                    IFNULL(SUM(adminAmount), 0) AS totalAdminRevenue,
                    COUNT(*) AS totalMinutes
                FROM billing_ledgers
            `;
            const [rows] = await pool.execute(sql);
            const r = rows[0] || {};
            return [{
                _id: null,
                usageRevenue: Number(r.usageRevenue),
                totalAstroPayout: Number(r.totalAstroPayout),
                totalAdminRevenue: Number(r.totalAdminRevenue),
                totalMinutes: Number(r.totalMinutes)
            }];
        } else if (this.modelName === 'Payment') {
            const sql = `
                SELECT IFNULL(SUM(amount), 0) AS totalCollected
                FROM payments
                WHERE status = 'success'
            `;
            const [rows] = await pool.execute(sql);
            const r = rows[0] || {};
            return [{
                _id: null,
                totalCollected: Number(r.totalCollected)
            }];
        }
        return [];
    }
}

// Mongoose-like Schema implementation
class Schema {
    constructor(definition, options) {
        this.definition = definition;
        this.options = options;
    }

    index() {
        // Suppress Mongoose indexing operations in SQL
        return this;
    }
}

Schema.Types = {
    Mixed: 'Mixed',
    ObjectId: 'ObjectId'
};

// Mongoose emulation object
const mongooseEmulator = {
    connect: async () => {
        // Verify database pool works
        const conn = await pool.getConnection();
        conn.release();
        console.log('✅ MySQL pool verified and active');
        return true;
    },
    connection: {
        readyState: 1, // Always connected
        on: (event, cb) => {
            // Emulate connection event hooks
            if (event === 'connected') {
                setTimeout(cb, 100);
            }
        },
        close: async () => {
            await pool.end();
            console.log('📡 MySQL connection pool closed gracefully');
            return true;
        },
        db: {
            listCollections: () => {
                return {
                    toArray: async () => {
                        const [rows] = await pool.execute("SHOW TABLES");
                        const key = Object.keys(rows[0] || {})[0];
                        if (!key) return [];
                        return rows.map(r => ({ name: r[key] }));
                    }
                };
            }
        }
    },
    Schema,
    model: (name, schema) => {
        return new CustomModel(name, schema);
    },
    // Raw pool access helper
    pool
};

module.exports = mongooseEmulator;
