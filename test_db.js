// test_db.js – simple MongoDB connection test
require('dotenv').config();
const { MongoClient } = require('mongodb');

const uri = process.env.MONGODB_URI;
if (!uri) {
  console.error('❌ MONGODB_URI not set in .env');
  process.exit(1);
}

(async () => {
  try {
    const client = new MongoClient(uri);
    await client.connect();
    console.log('✅ Connected to MongoDB successfully');
    // list databases as a quick sanity check
    const adminDb = client.db().admin();
    const dbs = await adminDb.listDatabases();
    console.log('Databases:', dbs.databases.map(d => d.name));
    await client.close();
  } catch (err) {
    console.error('❌ MongoDB connection error:', err.message);
    console.error(err);
  }
})();
