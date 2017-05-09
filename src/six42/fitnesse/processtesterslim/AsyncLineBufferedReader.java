package six42.fitnesse.processtesterslim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 * AsyncLineBufferedReader is a Runnable that eats lines from a BufferedReader
 * and stores them into a queue.
 */
public class AsyncLineBufferedReader implements Runnable{

    private LinkedList<String> lines;
    private BufferedReader reader;
    private boolean done = false;

    public AsyncLineBufferedReader(BufferedReader reader) {
      this.lines = new LinkedList<String>();
      this.reader = reader;
    }

    @Override
    public void run() {
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          lines.addLast(line);
        }
      } catch (IOException e) {
        lines.addLast("Exception:" + e.getMessage() + "\n");
      }
      done = true;
    }

  public String poll() throws Exception {
    return lines.poll();
    }

    public String[] getLines() {
      return lines.toArray(new String[0]);
    }

    public StringBuilder getAll()  {
      StringBuilder text = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
        text.append(lines.get(i)).append("\n");
      }
    return text;
    }
    
    public String getLine(int lineNo)  {
      if (lines.size() > lineNo){
        return lines.get(lineNo);
      }
      return null;
  }

}
