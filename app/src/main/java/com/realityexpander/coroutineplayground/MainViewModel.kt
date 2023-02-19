import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

//data class ViewState1<T>(val data: T? = null, val error: Throwable? = null) {
//    companion object {
//        fun <T> Success(data: T) = ViewState1(data)
//        fun <T> Error(error: Throwable) = ViewState1<T>(error = error)
//    }
//}

sealed interface ViewState<T> {
    data class Success<T>(val data: T) : ViewState<T>
    data class Error<T>(val error: Throwable) : ViewState<T>
    data class Loading<T>(val msg: String = "Loading...") : ViewState<T>
}

class MainViewModel : ViewModel() {

    val state = MutableStateFlow<ViewState<String>>(ViewState.Loading())

    private suspend fun fetchDataConsumesCancellation(action: suspend () -> String) =
        try {
            state.update { ViewState.Success(action()) }
        } catch (e: Exception) {
            if (e is CancellationException) {
                println("*** fetchDataConsumeCancellation CancellationException consumed")
                // note: not rethrowing the exception
                // This is not a good idea, because it will hide the cancellation exception,
                // and the parent coroutine will continue to run.
            } else {
                println("*** fetchDataConsumeCancellation regular exception $e")
                state.update { ViewState.Error(e) }
            }
        }

    private suspend fun fetchDataRethrowsCancellation(action: suspend () -> String) =
        try {
            state.update { ViewState.Success(action()) }
        } catch (e: Exception) {
            if (e is CancellationException) {
                println("*** fetchDataRethrowCancellation CancellationException rethrown $e")
                throw e // correct behavior is to rethrow the exception.
            } else {
                println("*** fetchDataRethrowCancellation regular exception $e")
                state.update { ViewState.Error(e) }
            }
        }

    val handler = CoroutineExceptionHandler { _, e ->
        println("*** CoroutineExceptionHandler got $e")
    }

    init {

        state.update { ViewState.Loading() }

        viewModelScope.launch {

            delay(100)

//            coroutineScope {
//                val parentScope = this

                try {

//                    coroutineScope {
                supervisorScope {

                        val containerScope = this
                        println("------------------------- INIT VIEWMODEL")

                        val firstJob = launch(handler) {
                            println("*** 1st job launch started...")
                            delay(1500)

//                        throw Exception("from 1st block launch") // crashes app

//                        cancel("from 1st block launch"); yield() // only cancels this job, not the whole scope. With yield, stops immediately.

//                        containerScope.cancel("containerScope cancel from 1st job launch"); yield() // simulate outer scope canceling this job.
//                            parentScope.cancel("parentScope cancel from 1st job launch"); yield() // simulate outer scope canceling this job.

                            println("*** 1st job launch finished.")
                        }
//                    firstJob.join()
                        firstJob.invokeOnCompletion {
                            println("*** 1st job launch completed. $it")
                        }

                        val secondJob = launch(handler) {
                            println("*** 2nd job launch started...")
                            delay(50)

//                    firstJob.cancel("from 2nd job launch")

                            // • FETCH 1
//                    fetchDataConsumesCancellation {
                            fetchDataRethrowsCancellation {  // waits for completion before moving on to next block
                                println("*** 2nd job fetchData 1 started...")

                                delay(50)
//                            throw Exception("fetchData 1 Regular Exception")  // will not propagate to outer scope, because it is caught by fetchDataRethrowsCancellation
//                                throw CancellationException("fetchData 1 CancellationException")  // will propagate to outer scope, because it is re-thrown by fetchDataRethrowsCancellation
//                            cancel(); yield() // only cancels this lambda


                                println("*** 2nd job fetchData 1 finished.")
                                "fetch 1 result"
                            }

//                        throw Exception("fetchData scope exception")
//                        throw CancellationException("fetchData scope exception")
//                        cancel(); yield() // only cancels this job, not the whole scope. With yield, stops immediately.

                            // • FETCH 2
//                    fetchDataConsumesCancellation {
                            fetchDataRethrowsCancellation {
                                println("*** 2nd job fetchData 2 started...")
                                delay(50)
                                println("*** 2nd job fetchData 2 finished")
                                "fetch 2 result"
                            }
                        }
//                    secondJob.join()
                        secondJob.invokeOnCompletion {
                            println("*** 2nd job launch completed. $it")
                        }

//                    val x = async {
//                        println("*** 3rd block async started...")
//
//                        delay(175)
//                        throw Exception("from 3rd block async") // exception is caught by supervisorScope
////                        throw CancellationException("from 3rd block async") // exception is caught by supervisorScope
//
//                        println("*** 3rd block async finished.")
//                        "async result"
//                    }.await()

//                delay(100)
//                cancel("from coroutineScope")
                    }

                } catch (e: CancellationException) {
                    println("*** coroutineScope/supervisorScope CancellationException $e") // this is when a coroutine is cancelled and not necessarily an error
                } catch (e: Exception) {
                    println("*** coroutineScope/supervisorScope regular exception $e") // maybe show error box
                }
//            }
        }


        if (false) {
            viewModelScope.launch {

                val handler = CoroutineExceptionHandler { _, e ->
                    println("*** CoroutineExceptionHandler got $e")
                }

                try {
                    val result = supervisorScope {
//                val result = coroutineScope {

                        val scope = this
                        delay(100)

//                    launch(handler) {
                        launch {
                            try {
                                println("*** launch started...")
                                delay(100)

                                scope.cancel("from launch") // will cancel all children
//                            cancel("from launch") // will only cancel this child
//                            yield()

                                //throw Exception("launch exception") // will be caught by supervisorScope
                                println("*** launch finished.") // will NOT run
                            }
//                        catch (e: CancellationException) {
//                            println("*** launch cancelled. e: $e")
//                            throw e
//                        }
                            catch (e: Exception) {
                                if (e is CancellationException) {
                                    println("*** launch cancelled. e: $e")
                                    throw e
                                } else
                                    println("*** launch failed. e: $e")
                            }
                        }


//                    // Runs parallel to completion - Exceptions must be caught in this scope OR CRASH
//                    launch {
//                        println("*** Step 1 started...") // runs to completion
//                        delay(200)
//
//                        //throw Exception("step 1")  // will be NOT be caught by supervisorScope & Crashes app
//
//                        try {
////                            scope.cancel("Some dang error!")
////                            yield()  // without this, cancel will NOT be processed & will this block run to completion.
////                            println("*** Step 1 after cancel")
//
////                            throw Exception("step 1")  // will be caught in this try/catch block
//                            println("*** Step 1 finished.") // runs to completion
//                        } catch (e: CancellationException) {
//                            println("*** Step 1 cancelled. e: $e")
////                            throw e // doesn't seem to make a difference here
//                        } catch (e: Exception) {
//                            println("*** Step 1 failed. e: $e")
//                        }
//
//                    }

                        // Runs parallel to completion - Exceptions are passed to parent scope
                        val result = async {
                            try {
                                println("*** Async started...")
                                delay(400)

                                //scope.cancel("from async") // will cancel all children
//                            cancel() // will cancel this child
//                            yield()

                                //throw Exception("async exception") // will be caught by supervisorScope
                                println("*** Async finished.")

                                "test result"
                            } catch (e: CancellationException) {
                                println("*** Async cancelled. e: $e")
//                            throw e
                            } catch (e: Exception) {
                                println("*** Async failed. e: $e")
                                //throw e
                            }
                        }

                        // Runs to completion before proceeding to next block
                        coroutineScope {
                            println("*** Step 2 started...")
                            delay(400)
                            //throw Exception("step 2")  // will be caught by supervisorScope
                            println("*** Step 2 finished.")
                        }

                        // Runs to completion before proceeding to next block
                        coroutineScope {
                            println("*** Step 3 started...")
                            delay(600)
                            println("*** Step 3 finished.")
                        }

//                    result.await()
                        "finished"
                    }

                    println("supervisorScope result: $result")

                    println("supervisorScope finished.")
                } catch (e: CancellationException) {
                    println("supervisorScope cancelled. e: $e")
                } catch (e: Exception) {
                    println("supervisorScope failed. e: $e")
                }
            }
        }
    }

}