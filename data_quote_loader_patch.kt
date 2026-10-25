// Patch QuoteRepository to load from assets/quotes.json at runtime (offline, no network)
// data/quotes/QuoteRepository.kt:1
@Singleton class QuoteRepository @Inject constructor(@ApplicationContext private val ctx: Context){
    private var cached: List<String>? = null
    fun getAll(): List<String>{
        cached?.let{ return it }
        return try{
            val json = ctx.assets.open("quotes.json").bufferedReader().readText()
            JSONArray(json).let{ arr-> List(arr.length()){ arr.getString(it) } }.also{ cached=it }
        } catch(e: Exception){ listOf("Wake up and conquer the day!") }
    }
    fun random(): String = getAll().random()
    fun randomNonRepeating(last: String?): String{
        val list = getAll().filter{ it!=last }
        return list.random()
    }
}
