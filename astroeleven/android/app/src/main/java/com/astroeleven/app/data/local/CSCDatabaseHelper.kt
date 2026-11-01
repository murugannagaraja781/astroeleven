package com.astroeleven.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class CSCDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "csc.db"
        const val DB_VERSION = 2
        private var DB_PATH = ""

        data class CityData(
            val id: String,
            val name: String,
            val stateId: String,
            val lat: String,
            val lon: String,
            val timezone: String = "Asia/Kolkata"
        )

        data class StateData(
            val id: String,
            val name: String,
            val countryId: String = "101"
        )

        val ALL_STATES = listOf(
            StateData("4035", "Tamil Nadu", "101"),
            StateData("4031", "Puducherry", "101"),
            StateData("4022", "Karnataka", "101"),
            StateData("4023", "Kerala", "101"),
            StateData("4008", "Andhra Pradesh", "101"),
            StateData("4036", "Telangana", "101"),
            StateData("4025", "Maharashtra", "101"),
            StateData("4014", "Delhi", "101"),
            StateData("4016", "Gujarat", "101"),
            StateData("4038", "Uttar Pradesh", "101"),
            StateData("4040", "West Bengal", "101"),
            StateData("4033", "Rajasthan", "101"),
            StateData("4024", "Madhya Pradesh", "101"),
            StateData("4032", "Punjab", "101"),
            StateData("4017", "Haryana", "101"),
            StateData("4011", "Bihar", "101"),
            StateData("4029", "Odisha", "101"),
            StateData("4007", "Goa", "101")
        )

        val ALL_CITIES = listOf(
            // Tamil Nadu (4035) - All 38 District Headquarters & Major Towns
            CityData("133647", "Chennai", "4035", "13.0827", "80.2707"),
            CityData("133648", "Coimbatore", "4035", "11.0168", "76.9558"),
            CityData("133649", "Madurai", "4035", "9.9252", "78.1198"),
            CityData("133650", "Tiruchirappalli (Trichy)", "4035", "10.7905", "78.7047"),
            CityData("133651", "Salem", "4035", "11.6643", "78.1460"),
            CityData("133652", "Tirunelveli", "4035", "8.7139", "77.7567"),
            CityData("133653", "Tiruppur", "4035", "11.1085", "77.3411"),
            CityData("133654", "Erode", "4035", "11.3410", "77.7172"),
            CityData("133655", "Vellore", "4035", "12.9165", "79.1325"),
            CityData("133656", "Thoothukudi (Tuticorin)", "4035", "8.7642", "78.1348"),
            CityData("133657", "Dindigul", "4035", "10.3673", "77.9803"),
            CityData("133658", "Thanjavur", "4035", "10.7870", "79.1378"),
            CityData("133659", "Ranipet", "4035", "12.9224", "79.3330"),
            CityData("133660", "Sivakasi", "4035", "9.4533", "77.7972"),
            CityData("133661", "Karur", "4035", "10.9601", "78.0766"),
            CityData("133662", "Udhagamandalam (Ooty)", "4035", "11.4102", "76.6950"),
            CityData("133663", "Hosur", "4035", "12.7409", "77.8253"),
            CityData("133664", "Nagercoil (Kanyakumari)", "4035", "8.1833", "77.4119"),
            CityData("133665", "Kanchipuram", "4035", "12.8342", "79.7036"),
            CityData("133666", "Cuddalore", "4035", "11.7480", "79.7714"),
            CityData("133667", "Kumbakonam", "4035", "10.9602", "79.3845"),
            CityData("133668", "Tiruvannamalai", "4035", "12.2253", "79.0747"),
            CityData("133669", "Pollachi", "4035", "10.6609", "77.0048"),
            CityData("133670", "Rajapalayam", "4035", "9.4532", "77.5545"),
            CityData("133671", "Gudiyatham", "4035", "12.9463", "78.8687"),
            CityData("133672", "Pudukkottai", "4035", "10.3797", "78.8208"),
            CityData("133673", "Vaniyambadi", "4035", "12.6825", "78.6200"),
            CityData("133674", "Ambur", "4035", "12.7904", "78.7166"),
            CityData("133675", "Nagapattinam", "4035", "10.7672", "79.8449"),
            CityData("133676", "Ramanathapuram", "4035", "9.3639", "78.8395"),
            CityData("133677", "Dharmapuri", "4035", "12.1211", "78.1582"),
            CityData("133678", "Krishnagiri", "4035", "12.5186", "78.2137"),
            CityData("133679", "Namakkal", "4035", "11.2189", "78.1674"),
            CityData("133680", "Perambalur", "4035", "11.2342", "78.8817"),
            CityData("133681", "Ariyalur", "4035", "11.1401", "79.0786"),
            CityData("133682", "Theni", "4035", "10.0104", "77.4768"),
            CityData("133683", "Virudhunagar", "4035", "9.5680", "77.9624"),
            CityData("133684", "Sivagangai", "4035", "9.8433", "78.4809"),
            CityData("133685", "Tenkasi", "4035", "8.9594", "77.3152"),
            CityData("133686", "Chengalpattu", "4035", "12.6819", "79.9888"),
            CityData("133687", "Tirupathur", "4035", "12.4925", "78.5678"),
            CityData("133688", "Kallakurichi", "4035", "11.7383", "78.9639"),
            CityData("133689", "Mayiladuthurai", "4035", "11.1018", "79.6522"),
            CityData("133690", "Viluppuram", "4035", "11.9401", "79.4861"),
            CityData("133691", "Tiruvarur", "4035", "10.7725", "79.6365"),
            CityData("133692", "Karaikudi", "4035", "10.0735", "78.7732"),
            CityData("133693", "Neyveli", "4035", "11.5976", "79.4861"),
            CityData("133694", "Arakkonam", "4035", "13.0783", "79.6677"),
            CityData("133695", "Tindivanam", "4035", "12.2338", "79.6517"),
            CityData("133696", "Pattukkottai", "4035", "10.4285", "79.3175"),
            CityData("133697", "Srivilliputhur", "4035", "9.5097", "77.6334"),
            CityData("133698", "Kovilpatti", "4035", "9.1724", "77.8681"),
            CityData("133699", "Palani", "4035", "10.4504", "77.5197"),
            CityData("133700", "Bodinayakanur", "4035", "10.0104", "77.3486"),
            CityData("133701", "Mettupalayam", "4035", "11.3000", "76.9400"),
            CityData("133702", "Arani", "4035", "12.6700", "79.2800"),
            CityData("133703", "Chidambaram", "4035", "11.3992", "79.6935"),
            CityData("133704", "Paramakudi", "4035", "9.5447", "78.5906"),
            CityData("133705", "Tiruchengode", "4035", "11.3789", "77.8967"),
            CityData("133706", "Coonoor", "4035", "11.3530", "76.7959"),
            CityData("133707", "Attur", "4035", "11.5982", "78.6006"),
            CityData("133708", "Gobichettipalayam", "4035", "11.4552", "77.4332"),
            CityData("133709", "Sankarankovil", "4035", "9.1717", "77.5342"),
            CityData("133710", "Aruppukkottai", "4035", "9.5126", "78.0988"),
            CityData("133711", "Ambasamudram", "4035", "8.7052", "77.4539"),
            CityData("133712", "Rameshwaram", "4035", "9.2876", "79.3129"),
            CityData("133713", "Kanyakumari", "4035", "8.0883", "77.5385"),
            CityData("133714", "Tambaram", "4035", "12.9249", "80.1000"),
            CityData("133715", "Avadi", "4035", "13.1147", "80.1018"),
            CityData("133716", "Manapparai", "4035", "10.6074", "78.4168"),
            CityData("133717", "Tiruttani", "4035", "13.1783", "79.6333"),
            CityData("133718", "Tiruvallur", "4035", "13.1432", "79.9074"),
            CityData("133719", "Cheyyar", "4035", "12.6586", "79.5422"),
            CityData("133720", "Walajapet", "4035", "12.9298", "79.3804"),

            // Puducherry (4031)
            CityData("140001", "Puducherry (Pondicherry)", "4031", "11.9416", "79.8083"),
            CityData("140002", "Karaikal", "4031", "10.9254", "79.8380"),

            // Karnataka (4022)
            CityData("140010", "Bengaluru (Bangalore)", "4022", "12.9716", "77.5946"),
            CityData("140011", "Mysuru (Mysore)", "4022", "12.2958", "76.6394"),
            CityData("140012", "Mangaluru (Mangalore)", "4022", "12.9141", "74.8560"),
            CityData("140013", "Hubballi-Dharwad", "4022", "15.3647", "75.1240"),
            CityData("140014", "Belagavi (Belgaum)", "4022", "15.8497", "74.4977"),
            CityData("140015", "Shivamogga (Shimoga)", "4022", "13.9299", "75.5681"),

            // Kerala (4023)
            CityData("140020", "Thiruvananthapuram (Trivandrum)", "4023", "8.5241", "76.9366"),
            CityData("140021", "Kochi (Cochin)", "4023", "9.9312", "76.2673"),
            CityData("140022", "Kozhikode (Calicut)", "4023", "11.2588", "75.7804"),
            CityData("140023", "Thrissur", "4023", "10.5276", "76.2144"),
            CityData("140024", "Kollam", "4023", "8.8932", "76.6141"),
            CityData("140025", "Palakkad", "4023", "10.7867", "76.6548"),
            CityData("140026", "Kannur", "4023", "11.8745", "75.3704"),
            CityData("140027", "Alappuzha (Alleppey)", "4023", "9.4981", "76.3388"),
            CityData("140028", "Kottayam", "4023", "9.5916", "76.5222"),

            // Andhra Pradesh (4008)
            CityData("140030", "Visakhapatnam", "4008", "17.6868", "83.2185"),
            CityData("140031", "Vijayawada", "4008", "16.5062", "80.6480"),
            CityData("140032", "Guntur", "4008", "16.3067", "80.4365"),
            CityData("140033", "Nellore", "4008", "14.4426", "79.9865"),
            CityData("140034", "Kurnool", "4008", "15.8281", "78.0373"),
            CityData("140035", "Tirupati", "4008", "13.6288", "79.4192"),
            CityData("140036", "Rajahmundry", "4008", "17.0005", "81.8040"),
            CityData("140037", "Kakinada", "4008", "16.9891", "82.2475"),
            CityData("140038", "Kadapa", "4008", "14.4673", "78.8242"),
            CityData("140039", "Anantapur", "4008", "14.6819", "77.6006"),

            // Telangana (4036)
            CityData("140040", "Hyderabad", "4036", "17.3850", "78.4867"),
            CityData("140041", "Warangal", "4036", "17.9689", "79.5941"),
            CityData("140042", "Nizamabad", "4036", "18.6725", "78.0941"),
            CityData("140043", "Karimnagar", "4036", "18.4386", "79.1288"),
            CityData("140044", "Khammam", "4036", "17.2473", "80.1514"),

            // Maharashtra (4025)
            CityData("140050", "Mumbai", "4025", "19.0760", "72.8777"),
            CityData("140051", "Pune", "4025", "18.5204", "73.8567"),
            CityData("140052", "Nagpur", "4025", "21.1458", "79.0882"),

            // Delhi (4014)
            CityData("140060", "New Delhi", "4014", "28.6139", "77.2090"),

            // West Bengal (4040)
            CityData("140070", "Kolkata", "4040", "22.5726", "88.3639")
        )
    }

    init {
        DB_PATH = context.applicationInfo.dataDir + "/databases/"
        copyDataBase()
    }

    private fun checkDataBase(): Boolean {
        val dbFile = File(DB_PATH + DB_NAME)
        return dbFile.exists() && dbFile.length() > 10 * 1024 * 1024
    }

    private fun copyDataBase() {
        if (!checkDataBase()) {
            this.readableDatabase
            this.close()
            try {
                copyDBFile()
            } catch (e: IOException) {
                android.util.Log.e("CSCDatabaseHelper", "Error copying database: ${e.message}")
                createEmptyTables()
            }
        }
    }

    @Throws(IOException::class)
    private fun copyDBFile() {
        try {
            val mInput: InputStream = context.assets.open(DB_NAME)
            val outFile = File(DB_PATH + DB_NAME)
            val mOutput: OutputStream = FileOutputStream(outFile)
            val mBuffer = ByteArray(64 * 1024)
            var mLength: Int
            while (mInput.read(mBuffer).also { mLength = it } > 0) {
                mOutput.write(mBuffer, 0, mLength)
            }
            mOutput.flush()
            mOutput.close()
            mInput.close()
        } catch (e: Exception) {
            android.util.Log.e("CSCDatabaseHelper", "Database file not found in assets, using comprehensive fallback data")
            throw IOException("Database file not found: $DB_NAME")
        }
    }

    private fun createEmptyTables() {
        var db: SQLiteDatabase? = null
        try {
            db = this.writableDatabase
            createTablesAndPopulate(db)
        } catch (e: Exception) {
            android.util.Log.e("CSCDatabaseHelper", "Error creating tables: ${e.message}")
        }
    }

    private fun createTablesAndPopulate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS countries (
                id TEXT PRIMARY KEY,
                name TEXT,
                iso2 TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS states (
                id TEXT PRIMARY KEY,
                name TEXT,
                country_id TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS cities (
                id TEXT PRIMARY KEY,
                name TEXT,
                state_id TEXT,
                latitude TEXT,
                longitude TEXT,
                timezone TEXT
            )
        """)

        populateDefaultData(db)
    }

    private fun populateDefaultData(db: SQLiteDatabase) {
        try {
            db.beginTransaction()

            // Country
            db.execSQL("INSERT OR REPLACE INTO countries VALUES ('101', 'India', 'IN')")

            // States
            for (state in ALL_STATES) {
                db.execSQL(
                    "INSERT OR REPLACE INTO states (id, name, country_id) VALUES (?, ?, ?)",
                    arrayOf(state.id, state.name, state.countryId)
                )
            }

            // Cities
            for (city in ALL_CITIES) {
                db.execSQL(
                    "INSERT OR REPLACE INTO cities (id, name, state_id, latitude, longitude, timezone) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(city.id, city.name, city.stateId, city.lat, city.lon, city.timezone)
                )
            }

            db.setTransactionSuccessful()
            android.util.Log.i("CSCDatabaseHelper", "Successfully populated ${ALL_CITIES.size} cities and ${ALL_STATES.size} states.")
        } catch (e: Exception) {
            android.util.Log.e("CSCDatabaseHelper", "Error populating default data: ${e.message}")
        } finally {
            try {
                db.endTransaction()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.let { createTablesAndPopulate(it) }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let { createTablesAndPopulate(it) }
    }

    fun getCountries(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        try {
            val db = this.readableDatabase
            ensureDataAvailable(db)
            val cursor = db.rawQuery("SELECT * FROM countries ORDER BY name", null)
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val map = mutableMapOf<String, String>()
                        map["id"] = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                        map["name"] = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        map["iso2"] = cursor.getString(cursor.getColumnIndexOrThrow("iso2"))
                        list.add(map)
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor.close()
            }
        } catch (e: Exception) {
            android.util.Log.e("CSCDatabaseHelper", "Error reading countries: ${e.message}")
        }
        return list
    }

    fun getStates(countryId: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        try {
            val db = this.readableDatabase
            ensureDataAvailable(db)
            val cursor = db.rawQuery("SELECT * FROM states WHERE country_id = ? ORDER BY name", arrayOf(countryId))
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val map = mutableMapOf<String, String>()
                        map["id"] = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                        map["name"] = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        list.add(map)
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor.close()
            }
        } catch (e: Exception) {
            android.util.Log.e("CSCDatabaseHelper", "Error reading states: ${e.message}")
        }
        return list
    }

    fun getCities(stateId: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        try {
            val db = this.readableDatabase
            ensureDataAvailable(db)
            val cursor = db.rawQuery("SELECT * FROM cities WHERE state_id = ? ORDER BY name", arrayOf(stateId))
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val map = mutableMapOf<String, String>()
                        map["id"] = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                        map["name"] = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        map["latitude"] = cursor.getString(cursor.getColumnIndexOrThrow("latitude"))
                        map["longitude"] = cursor.getString(cursor.getColumnIndexOrThrow("longitude"))
                        val tzIndex = cursor.getColumnIndex("timezone")
                        if (tzIndex != -1) {
                            map["timezone"] = cursor.getString(tzIndex) ?: "Asia/Kolkata"
                        } else {
                            map["timezone"] = "Asia/Kolkata"
                        }
                        list.add(map)
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor.close()
            }
        } catch (e: Exception) {
            android.util.Log.e("CSCDatabaseHelper", "Error reading cities: ${e.message}")
        }
        return list
    }

    private fun ensureDataAvailable(db: SQLiteDatabase) {
        try {
            val cursor = db.rawQuery("SELECT count(*) FROM cities WHERE state_id = '4035'", null)
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()

            // If existing db only has the old 3 cities or is empty, populate all cities immediately
            if (count < 10) {
                populateDefaultData(db)
            }
        } catch (e: Exception) {
            android.util.Log.e("CSCDatabaseHelper", "Error in ensureDataAvailable: ${e.message}")
            try {
                createTablesAndPopulate(db)
            } catch (ignored: Exception) {}
        }
    }
}
