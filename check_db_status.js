// check_db_status.js – prints MongoDB connection readyState
require('dotenv').config();
const mongoose = require('mongoose');

const uri = process.env.MONGODB_URI || 'mongodb://localhost:27017/astroeleven';

(async () => {
  try {
    await mongoose.connect(uri);
    console.log('✅ Connected, readyState =', mongoose.connection.readyState);
    await mongoose.disconnect();
    console.log('✅ Disconnected');
  } catch (err) {
    console.error('❌ Connection error:', err.message);
  }
})();
