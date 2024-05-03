package com.ziqiphyzhou.flashcard.shared

/*

*Notes on coroutines*
- suspend fun example() {}
-- they are simply functions marked with breakpoints
-- they execute like a normal function, and the behavior feels synchronous
-- it only returns when all contents finish
- fun without suspend marker
-- fun without suspend marker can finish before its contents launched in new coroutines
- coroutineScope {}
-- a coroutine scope marks a range in functions
-- a coroutine scope is finished only when all contents finish
- withContext(dispatchers.IO) {}
-- behaves as if synchronous
-- moves included codes to run in another thread
- finishes only when all contents finish
- CoroutineScope(dispatchers.IO).launch {}
-- launches a new coroutine in the specified optional context
-- when done, coroutine runs freely, which is dangerous
-- a job object is created when launch is called, one can handle the job with cancel()
-- launched codes needs external handling of some coroutine scope

 */