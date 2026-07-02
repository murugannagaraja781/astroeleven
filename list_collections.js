// list_collections.js – prints all collection names in the configured DB
require('dotenv').config();
const mongoose = require('mongoose');

const uri = process.env.MONGODB_URI || 'mongodb://localhost:27017/astroeleven';

(async () => {
  try {
    await mongoose.connect(uri, { serverSelectionTimeoutMS: 5000, socketTimeoutMS: 5000 });
    const db = mongoose.connection.db;
    const collections = await db.listCollections().toArray();
    console.log('📚 Collections in the database:');
    collections.forEach(col => console.log('- ' + col.name));
    await mongoose.disconnect();
  } catch (err) {
    console.error('❌ Error listing collections:', err.message);
    process.exit(1);
  }
})();
