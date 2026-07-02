require('dotenv').config();
const mongoose = require('mongoose');
const User = require('./models/User');

async function test() {
    await mongoose.connect(process.env.MONGODB_URI);
    const astros = await User.find({ role: 'astrologer' });
    console.log("Total Astrologers:", astros.length);
    console.log("Astrologers:", JSON.stringify(astros, null, 2));
    process.exit(0);
}
test();
