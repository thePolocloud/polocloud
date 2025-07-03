package dev.httpmarco.polocloud.agent.detector

import dev.httpmarco.polocloud.agent.logger

class DetectorFactoryThread(detector: Detector) {

    private val thread = Thread {
        while (true) {
            try {
                detector.tick()
                Thread.sleep(detector.cycleLife())
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                logger.throwable(e)
            }
        }
    }

    companion object {
        fun bindDetector(detector: Detector) :DetectorFactoryThread {
            return DetectorFactoryThread(detector)
        }
    }

    fun detect() {
        thread.start()
    }

    fun close(){
        thread.interrupt()
    }
}