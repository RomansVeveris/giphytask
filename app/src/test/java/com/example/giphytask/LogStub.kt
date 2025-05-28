package android.util

// stub simulating Log.e(), because android Log is not available in tests
object Log {
    @JvmStatic
    fun e(tag: String?, msg: String?, tr: Throwable? = null): Int {
        return 0
    }
}