package com.bcpl.coroutinejobs

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import com.bcpl.coroutinejobs.databinding.ActivityMainBinding
import kotlinx.coroutines.*

const val PROGRESS_MAX = 100
const val PROGRESS_START = 0
const val JOB_TIME = 4000 // ms

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding;
    private lateinit var job: CompletableJob   // more options than Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
    }

    private fun setupUi() {
        binding.jobButton.setOnClickListener {
            if (!::job.isInitialized) {
                initJob()
            }
            binding.jobProgressBar.startJobOrCancel(job)
        }
    }

    // extension fun, access this == ProgressBar and its props / methods
    fun ProgressBar.startJobOrCancel(job: Job) {
        if (this.progress > 0) {
            println("$job is already active. Cancelling..")
            resetJob()
        } else {
          binding.jobButton.setText("Cancel job #1")
          /*
          Dispatchers.XX + job, that means, own isolated environment for that exact job.
          This enables possibility of cancels only precisely job:
          ex:
          val scope = CoroutineScope(Dispatchers.IO + job).launch {}
          scope.cancel() - cancels only this one job

          val job1 = Job()
          val job2 = Job()
          CoroutineScope(Dispatchers.IO).launch {}


          val scope = CoroutineScope(Dispatchers.IO).launch {}
          scope.cancel() - cancels all jobs in scope
          */
          CoroutineScope(Dispatchers.IO + job).launch {
            println("coroutine $this is activated with job $job")

              for (i in PROGRESS_START.. PROGRESS_MAX) {
                  delay((JOB_TIME / PROGRESS_MAX).toLong())
                  this@startJobOrCancel.progress = i
              }
              updateJobCompleteTextView("Job is complete")
          }
        }
    }

    private fun resetJob() {
        if (job.isActive || job.isCompleted) {
            // will be caught on: job.invokeOnCompletion
            job.cancel(CancellationException("Resetting job"))
        }
        // once cancelled, need initialisation
        initJob()
    }

    private fun updateJobCompleteTextView(text: String) {
        GlobalScope.launch(Dispatchers.Main) {
            binding.jobCompleteText.setText(text)
        }
    }

    fun initJob() {
        binding.jobButton.setText("Start Job #1")
        updateJobCompleteTextView("")
        job = Job()
        // if job completed or canceled this will be fired
        job.invokeOnCompletion {
            it?.message.let {
                var msg = it
                if (msg.isNullOrBlank()) {
                    msg = "Unknown cancellation error."
                }
                println("$job was cancelled. Reason: $msg")
                showToast(msg)
            }
        }
        binding.jobProgressBar.max = PROGRESS_MAX
        binding.jobProgressBar.progress = PROGRESS_START
    }

    fun showToast(text: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
        }
    }
}