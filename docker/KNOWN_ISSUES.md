## Known Docker Issues

- Control characters or other artefacts when outputting logs or attaching to the container:

  ```makefile
  % user@hostname:/srv/polocloud$ docker run ...

  ...more PoloCloud logs....

  03:26:02 | INFO - Service lobby-1 is ONLINE — Paper 26.2 (protocol 776), 0/20 players, 1ms
  % user@hostname:/srv/polocloud$ 2027;0$y1;2c
  ```

  In this example the **2027;0$y1;2c** is the problem only comming from poloclout into my terminal input on a cachyos desktop and a ubuntu server host.

- JRE sometimes crashes when attaching to the container:
  ```makefile
  # A fatal error has been detected by the Java Runtime Environment:
  #
  #  Internal Error (upcallLinker.cpp:77), pid=7, tid=8
  #  guarantee(thread->thread_state() == _thread_in_native) failed: wrong thread state for upcall
  #
  # JRE version: OpenJDK Runtime Environment Zulu25.36+205-CA (25.0.4.1+1) (build 25.0.4.1+1-LTS)
  # Java VM: OpenJDK 64-Bit Server VM Zulu25.36+205-CA (25.0.4.1+1-LTS, mixed mode, sharing, tiered, compressed oops, compressed class ptrs, g1 gc, linux-amd64)
  # Problematic frame:
  # V  [libjvm.so+0x1157008]  UpcallLinker::on_entry(UpcallStub::FrameData*)+0x1a8
  #
  # Core dump will be written. Default location: Determined by the following: "/usr/lib/systemd/systemd-coredump %P %u %g %s %t %c %h %d %F %I" (alternatively, falling back to /data/core.7)
  #
  # An error report file with more information is saved as:
  # /data/hs_err_pid7.log
  [182.533s][warning][os] Loading hsdis library failed
  #
  # If you would like to submit a bug report, please visit:
  #   http://www.azul.com/support/
  ```
  This just happens sometimes and we can security reproduce it. AI sayed `JAVA_TOOL_OPTIONS=-Dorg.jline.terminal.ffm=false` but it didnt helped.
  I always get it on a cachyos desktop when attachting to a container.
  It also sometimes happens on a ubuntu server host.

## Help us improve

[CONTRIBUTING](../.github/CONTRIBUTING.md)
