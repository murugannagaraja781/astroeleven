const { MongoClient } = require('mongodb');
const uri = "mongodb+srv://pdhanalakshmi357_db_user:UC0grZ88PKkrYmGr@cluster0.rs39etx.mongodb.net/?appName=Cluster0";
const client = new MongoClient(uri);

async function run() {
  try {
    await client.connect();
    const db = client.db('test'); // Or whatever the DB name is, we'll try 'test' or find out
    // list collections
    const collections = await db.listCollections().toArray();
    console.log("Collections:", collections.map(c => c.name));
    
    // search users
    const users = await db.collection('users').find({}).limit(5).toArray();
    console.log("Users:", users);

    // search admin or teammember
    const team = await db.collection('teammembers').find({}).toArray();
    console.log("Team members:", team);
    
    // try astrology DB if test is wrong
    const dbs = await client.db().admin().listDatabases();
    console.log("Databases:", dbs.databases.map(d => d.name));

  } finally {
    await client.close();
  }
}
run().catch(console.dir);
