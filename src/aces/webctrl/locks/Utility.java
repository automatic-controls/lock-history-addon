package aces.webctrl.locks;
import java.io.*;
import java.util.*;
import java.util.regex.*;
public class Utility {
  private final static Pattern lineEnding = Pattern.compile("\\r?+\\n");
  /**
   * This method is provided for compatibility with older JRE versions.
   * Newer JREs already have a built-in equivalent of this method: {@code InputStream.readAllBytes()}.
   * @return a {@code byte[]} array containing all remaining bytes read from the {@code InputStream}.
   */
  public static byte[] readAllBytes(InputStream s) throws IOException {
    ArrayList<byte[]> list = new ArrayList<byte[]>();
    int len = 0;
    byte[] buf;
    int read;
    while (true){
      buf = new byte[8192];
      read = s.read(buf);
      if (read==-1){
        break;
      }
      len+=read;
      list.add(buf);
      if (read!=buf.length){
        break;
      }
    }
    byte[] arr = new byte[len];
    int i = 0;
    for (byte[] bytes:list){
      read = Math.min(bytes.length,len);
      len-=read;
      System.arraycopy(bytes, 0, arr, i, read);
      i+=read;
    }
    return arr;
  }
  /**
   * Loads all bytes from the given resource and convert to a {@code UTF-8} string.
   * @return the {@code UTF-8} string representing the given resource.
   */
  public static String loadResourceAsString(String name) throws Throwable {
    byte[] arr;
    try(
      InputStream s = Utility.class.getClassLoader().getResourceAsStream(name);
    ){
      arr = readAllBytes(s);
    }
    return lineEnding.matcher(new String(arr, java.nio.charset.StandardCharsets.UTF_8)).replaceAll(System.lineSeparator());
  }
  /**
   * Loads all bytes from the given resource and convert to a {@code UTF-8} string.
   * @return the {@code UTF-8} string representing the given resource.
   */
  public static String loadResourceAsString(ClassLoader cl, String name) throws Throwable {
    byte[] arr;
    try(
      InputStream s = cl.getResourceAsStream(name);
    ){
      arr = readAllBytes(s);
    }
    return lineEnding.matcher(new String(arr, java.nio.charset.StandardCharsets.UTF_8)).replaceAll(System.lineSeparator());
  }
  /**
   * Encodes a JSON string.
   */
  public static String escapeJSON(String s){
    if (s==null){ return "NULL"; }
    int len = s.length();
    StringBuilder sb = new StringBuilder(len+16);
    char c;
    String hex;
    int hl;
    for (int i=0;i<len;++i){
      c = s.charAt(i);
      switch (c){
        case '\\': case '/': case '"': {
          sb.append('\\').append(c);
          break;
        }
        case '\n': {
          sb.append("\\n");
          break;
        }
        case '\t': {
          sb.append("\\t");
          break;
        }
        case '\r': {
          sb.append("\\r");
          break;
        }
        case '\b': {
          sb.append("\\b");
          break;
        }
        case '\f': {
          sb.append("\\f");
          break;
        }
        default: {
          if (c>31 && c<127){
            sb.append(c);
          }else{
            //JDK17: hex = HexFormat.of().toHexDigits(c);
            hex = Integer.toHexString((int)c);
            hl = hex.length();
            if (hl<=4){
              sb.append("\\u");
              for (;hl<4;hl++){
                sb.append('0');
              }
              sb.append(hex);
            }
          }
        }
      }
    }
    return sb.toString();
  }
}