package six42.fitnesse.processtesterslim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class ProcessTesterSlim {

  public class ChildProcess {
    public AsyncLineBufferedReader stdoutStream;
    public AsyncLineBufferedReader stderrStream;
    public Process process;
    private String exitValue = "";
    public String command = "";

    public void execute(String command) throws IOException {
      if (isVerbose)
        System.out.println("command = " + command);
      this.command = command;
      this.process = Runtime.getRuntime().exec(command);
      this.exitValue = "RUNNING";
      this.stdoutStream = makeStream(process.getInputStream());
      this.stderrStream = makeStream(process.getErrorStream());
    }

    public void execute(List<String> command) throws IOException {
        if (isVerbose)
          System.out.println("command = " + command.toString());
        this.command = command.toString();
        ProcessBuilder pb  = new ProcessBuilder();
        pb.command(command);
        this.process = pb.start();
        this.exitValue = "RUNNING";
        this.stdoutStream = makeStream(process.getInputStream());
        this.stderrStream = makeStream(process.getErrorStream());
      }

    private AsyncLineBufferedReader makeStream(InputStream s) {
      AsyncLineBufferedReader gatherer = new AsyncLineBufferedReader(new BufferedReader(
          new InputStreamReader(s)));
      new Thread(gatherer).start();
      return gatherer;
    }

    public long waitForText(String streamId, String target) throws Exception {
      return waitForText(streamId, target, "1:3000", 0);
    }

    public long waitForText(String streamId, String target, String retryConfig)
        throws Exception {
      return waitForText(streamId, target, retryConfig, 0);
    }

    public long waitForText(String streamId, String target, String retryConfig,
        int startLine) throws Exception {
      String line;
      AsyncLineBufferedReader gatherer = getStream(streamId);
      RetryManager retry;
      int l = startLine;
      for (retry = new RetryManager(retryConfig); retry.shouldTryAgain();) {
        while ((line = gatherer.getLine(l)) != null) {
          if (line.indexOf(target) >= 0) {
            return l;
          }
          l++;
        }
        retry.runFailed();
      }
      return -1;
    }

    public AsyncLineBufferedReader getStream(String streamId) throws Exception {
      if (streamId.equalsIgnoreCase("stdout")) {
        return stdoutStream;
      } else if (streamId.equalsIgnoreCase("stderr")) {
        return stderrStream;
      } else {
        throw new Exception("Stream id not 'stdout' or 'stderr'");
      }
    }

    public void flush() throws Exception {
      flush(stdoutStream);
      flush(stderrStream);
    }

    private StringBuilder flush(AsyncLineBufferedReader gatherer) throws Exception {
      String line;
      StringBuilder text = new StringBuilder();
      while ((line = gatherer.poll()) != null) {
        text.append(line).append("\n");
      }

      return text;
    }

    public void terminate() {
      try {
        process.waitFor(100, TimeUnit.MILLISECONDS);
        exitValue = String.valueOf(process.exitValue());
        return;
      } catch (IllegalThreadStateException e1) {
        exitValue = "KILL";
        process.destroy();
        process.destroyForcibly();
      } catch (InterruptedException e) {
        e.printStackTrace();
        exitValue = "KILL_INTERRUPTED";
        process.destroyForcibly();
      }
    }

    public String waitForTermination() throws Exception {
      int status = process.waitFor();
      exitValue = String.valueOf(status);
      return exitValue;
    }

    public boolean waitFor(double timeout) throws Exception {
      if (process.waitFor((new Double(timeout * 1000).longValue()),
          TimeUnit.MILLISECONDS)) {
        try {
          exitValue = String.valueOf(process.exitValue());
          return true;
        } catch (IllegalThreadStateException e1) {
          exitValue = "UNDEFINED";
          return true;
        }
      }
      exitValue = "RUNNING";
      return false;
    }

    public String waitForSeconds(double timeout) throws Exception {
      waitFor(timeout);
      return exitValue;
    }
    public String exitValue() {
      return exitValue;
    }

    public String stdErr() {
      return stderrStream.getAll().toString();
    }

    public String stdOut() {
      return stdoutStream.getAll().toString();
    }
  }
  
  private static HashMap<String, ChildProcess> commandProcessMap = new HashMap<String, ChildProcess>();

  private boolean isVerbose = false;
  
  
  public ProcessTesterSlim() {
    isVerbose = false;
  }

  public ProcessTesterSlim(boolean verbose) {
    isVerbose = verbose;
  }

  public String terminateAllProcesses() throws Exception {
    
    HashMap<String,String> ExitMap = new HashMap<String,String>();
    String alive;
    Set<String> processIds = commandProcessMap.keySet();
    for (Iterator<String> iterator = processIds.iterator(); iterator.hasNext();) {
      String processId = iterator.next();
      ChildProcess p = commandProcessMap.get(processId);
      if (p.process.isAlive()) {
        p.terminate();
        alive = "A ";
      } else
        alive = "_ ";
      ExitMap.put(processId,
          alive
 + p.exitValue() + ":"
              + (p.command).subSequence(0, Math.min(50, (p.command).length()))
);
    }
    if (isVerbose)
      System.out.println("ProcessTesterSlim: Inside terminateAllProcesses: "
          + ExitMap.toString());

    return ExitMap.toString();
  }
  
  
  /**
   * Starts a command but does not wait for it to finish.
   */

  public ChildProcess doSpawn(String command) throws IOException {
    String processId = generateUniqueProcessId();
    ChildProcess p = new ChildProcess();
    p.execute(command);
    commandProcessMap.put(processId, p);
    return p;
  }

  public ChildProcess doSpawnL(List<String> command) throws IOException {
	    String processId = generateUniqueProcessId();
	    ChildProcess p = new ChildProcess();
	    p.execute(command);
	    commandProcessMap.put(processId, p);
	    return p;
	  }

  /**
   * Starts a command and wait some time until it is finished. If the command
   * doesn't finishes in the given time it will be terminated
   */
  public ChildProcess execute(String command, double timeout) throws Exception {
    String processId = generateUniqueProcessId();
    ChildProcess p = new ChildProcess();
    p.execute(command);
    commandProcessMap.put(processId, p);
    if (!p.waitFor(timeout)) {
      p.terminate();
    };
    commandProcessMap.remove(processId);
    return p;
  }

  public ChildProcess executeL(List<String> command, double timeout) throws Exception {
	    String processId = generateUniqueProcessId();
	    ChildProcess p = new ChildProcess();
	    p.execute(command);
	    commandProcessMap.put(processId, p);
	    if (!p.waitFor(timeout)) {
	      p.terminate();
	    };
	    commandProcessMap.remove(processId);
	    return p;
	  }

  public ChildProcess execute(String command) throws Exception {
    return execute( command, 10.0);
  }
  
  public ChildProcess executeL(List<String> command) throws Exception {
	    return executeL( command, 10.0);
	  }
  public void pause(int seconds) throws Exception {
    int milliseconds = 1000 * seconds;
    Thread.sleep(milliseconds);
  }

  private String generateUniqueProcessId() {
    String processId;
    processId = "blank" + Math.random();
    return processId;
  }

  public void attachShutDownHook(){
    Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
        try {
          terminateAllProcesses();
        } catch (Exception e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
        }
        if (isVerbose)
          System.out.println("ProcessTesterSlim: Inside Shutdown Hook");
        }
    });
    if (isVerbose)
      System.out.println("ProcessTesterSlim: Shut Down Hook Attached.");
  }
  

}
