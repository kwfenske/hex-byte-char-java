/*
  Hexadecimal Byte Character #1 - Convert Encoded Data Bytes to Character Text
  Written by: Keith Fenske, http://kwfenske.github.io/
  Monday, 13 June 2022
  Java class name: HexByteChar1
  Copyright (c) 2022 by Keith Fenske.  Apache License or GNU GPL.

  HexByteChar is a Java 1.4 graphical (GUI) application to convert between
  binary data bytes and text characters, in different character sets or
  encodings.  Please refer to the following web page:

      http://en.wikipedia.org/wiki/Character_encoding

  Data bytes are shown in hexadecimal on the left side of a split screen.  Text
  characters are shown on the right as per the Unicode standard.  You may enter
  hex data on the left, select an encoding, and click the "Convert Bytes to
  Text" button to see text on the right.  You may enter text on the right,
  select an encoding, and click the "Convert Text to Bytes" button to see hex
  data on the left.  Edit in the usual manner.  The hex display looks best with
  the "Lucida Console" font installed.

  This program was originally written to identify strange UTF-8 characters in
  e-mail messages.  Success was mixed because of the number of steps required:
  copy and paste unknown characters as text, convert text to data bytes using
  the system's default encoding, convert bytes back to text as UTF-8, convert
  text again as UTF-16 or UTF-32, and look up resulting byte codes to identify
  Unicode characters.  (See any "Character Map" application.)  A single button
  to do this would be more convenient but much too specialized.  The strange
  character was often "U+FEFF zero width no-break space" encoded as 0xEF 0xBB
  0xBF, also known as a byte-order mark.

  Apache License or GNU General Public License
  --------------------------------------------
  HexByteChar1 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options for the position and size of the
  application window, and the size of the display font.  See the "-?" option
  for a help summary:

      java  HexByteChar1  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u16 or -u18 is recommended because the default
  Java font is too small.

  Restrictions and Limitations
  ----------------------------
  For copy and paste to another application on Windows, a null character (0x00)
  may terminate a text string, even when it appears in the middle.  Both 0x0A
  and 0x0D are newline characters, whose exact representation depends upon the
  local system.

  Suggestions for New Features
  ----------------------------
  (1) Comments in this program and its documentation have two spaces between
      sentences.  That is an old tradition from the days of manual typewriters
      (source code being inherently monospaced).  Java removes only a single
      space in a JTextArea with the "word wrap" attribute set, so our text
      strings have one space between sentences, while comments have two.  KF,
      2022-06-13.
  (2) Someone somewhere will want to copy and paste binary data bytes via the
      clipboard (and not in their hexadecimal form).  KF, 2022-06-17.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support
import javax.swing.border.*;      // decorative borders

public class HexByteChar1
{
  /* constants */

  static final int BYTE_MASK = 0x000000FF; // gets low-order byte from integer
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2022 by Keith Fenske. Apache License or GNU GPL.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
//static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'}; // hexadecimal digits
  static final String LOCAL_ENCODING = "(local default)";
                                  // our special name for local character set
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Convert Encoded Data Bytes to Character Text - by: Keith Fenske";
  static final String RAW_ENCODING = "(raw data bytes)";
                                  // our special name for no data encoding
  static final String SYSTEM_FONT = "Dialog"; // this font is always available

  /* class variables */

  static JTextArea byteField;     // data bytes displayed in hexadecimal
  static JTextArea charField;     // text characters displayed in Unicode
  static JButton clearByteButton, clearCharButton, convertByteButton,
    convertCharButton, copyByteButton, copyCharButton, exitButton; // buttons
  static JComboBox encodeDialog;  // user's choice for character set encoding
  static JFrame mainFrame;        // this application's window if GUI
  static boolean mswinFlag;       // true if running on Microsoft Windows

/*
  main() method

  We run as a graphical application only.  Set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Font buttonFont;              // font for buttons, labels, status, etc
    Font byteFont;                // font for hexadecimal data bytes only
    Border emptyBorder;           // remove borders around text areas
    String encodeName;            // select name of character set encoding
    String fontName;              // preferred font name for hex data bytes
    int fontSize;                 // preferred font size or chosen by user
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    encodeName = "UTF-8";         // most common character set in the world
    fontName = "Lucida Console";  // many systems have this font installed
    fontSize = 18;                // preferred font size (user may change)
    mainFrame = null;             // during setup, there is no GUI window
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Check command-line parameters for options. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      else if (word.startsWith("-e") || (mswinFlag && word.startsWith("/e")))
        encodeName = args[i].substring(2); // accept anything for encoding

      else if (word.startsWith("-f") || (mswinFlag && word.startsWith("/f")))
        fontName = args[i].substring(2); // accept anything for font name

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        try                       // try to parse remainder as unsigned integer
        {
          fontSize = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          fontSize = -1;          // set result to an illegal value
        }
        if ((fontSize < 10) || (fontSize > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(-?\\d{1,5})\\s*,\\s*(-?\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x"))
        || word.equals("-x1") || (mswinFlag && word.equals("/x1")))
      {
        maximizeFlag = true;      // yes, maximize our main window
      }
      else if (word.equals("-x0") || (mswinFlag && word.equals("/x0")))
        maximizeFlag = false;     // regular window, don't maximize

      else                        // parameter is not a recognized option
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }
    }

    /* Initialize shared graphical objects. */

    action = new HexByteChar1User(); // create our shared action listener
    buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, fontSize);
    byteFont = new Font(fontName, Font.PLAIN, fontSize);
    emptyBorder = BorderFactory.createEmptyBorder(); // for removing borders

    /* Create the graphical interface as a series of smaller panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel261, label354, etc). */

    JPanel panel21 = new JPanel(new BorderLayout(0, 10));

    /* The top has control buttons and the character set encoding.  BoxLayout
    expands JComboBox vertically to match JButton in size and base line (text).
    FlowLayout centers but does not allow JComboBox to expand horizontally. */

    JPanel panel31 = new JPanel();
    panel31.setLayout(new BoxLayout(panel31, BoxLayout.X_AXIS));

    convertByteButton = new JButton("Convert Bytes to Text");
    convertByteButton.addActionListener(action);
    convertByteButton.setFont(buttonFont);
    convertByteButton.setMnemonic(KeyEvent.VK_B);
    convertByteButton.setToolTipText(
      "Convert encoded bytes to Unicode text characters.");
    panel31.add(convertByteButton);
    panel31.add(Box.createHorizontalStrut(40));

    encodeDialog = new JComboBox();
    encodeDialog.addItem(LOCAL_ENCODING); // start with our special names
    encodeDialog.addItem(RAW_ENCODING);
    Object[] list32 = java.nio.charset.Charset.availableCharsets().keySet()
      .toArray();                 // get character set names from local system
    for (i = 0; i < list32.length; i ++)
      encodeDialog.addItem((String) list32[i]); // insert each encoding name
    encodeDialog.setEditable(true); // allow user to enter alternate names
    encodeDialog.setFont(buttonFont);
//  encodeDialog.setMnemonic(KeyEvent.VK_E); // not supported for JComboBox
//  encodeDialog.setOpaque(false); // doesn't work, would be a nice touch
    encodeDialog.setSelectedItem(encodeName); // selected item is our default
    encodeDialog.setToolTipText("Select name of character set encoding.");
//  encodeDialog.addActionListener(action); // do last so don't fire early
    panel31.add(encodeDialog);
    panel31.add(Box.createHorizontalStrut(40));

    convertCharButton = new JButton("Convert Text to Bytes");
    convertCharButton.addActionListener(action);
    convertCharButton.setFont(buttonFont);
    convertCharButton.setMnemonic(KeyEvent.VK_T);
    convertCharButton.setToolTipText(
      "Convert Unicode text characters to encoded bytes.");
    panel31.add(convertCharButton);
    panel31.add(Box.createHorizontalStrut(40));

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel31.add(exitButton);

    JPanel panel33 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    panel33.add(panel31);         // center without horizontal expansion
    panel21.add(panel33, BorderLayout.NORTH);

    /* The middle has text areas for hex data bytes and text characters. */

    byteField = new JTextArea(18, 24);
    byteField.setEditable(true);
    byteField.setFont(byteFont);
    byteField.setLineWrap(true);
    byteField.setMargin(new Insets(4, 7, 4, 7)); // top, left, bottom, right
    byteField.setWrapStyleWord(true);
    JScrollPane panel52 = new JScrollPane(byteField);
    panel52.setBorder(emptyBorder); // no border necessary here

    charField = new JTextArea(18, 20);
    charField.setEditable(true);
    charField.setFont(buttonFont);
    charField.setLineWrap(true);
    charField.setMargin(new Insets(4, 7, 4, 7));
    charField.setWrapStyleWord(true);
    charField.setText("HexByteChar is a Java 1.4 graphical (GUI) application"
      + " to convert between binary data bytes and text characters, in"
      + " different character sets or encodings. Enter hex data on the left,"
      + " select an encoding, and click the \"Convert Bytes to Text\" button"
      + " to see text on the right. Enter text on the right, select an"
      + " encoding, and click the \"Convert Text to Bytes\" button to see hex"
      + " data on the left. Edit in the usual manner."
      + "\n\nCopyright (c) 2022 by Keith Fenske. By using this program, you"
      + " agree to terms and conditions of the Apache License and/or GNU"
      + " General Public License.");
    JScrollPane panel53 = new JScrollPane(charField);
    panel53.setBorder(emptyBorder);

    JSplitPane panel54 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel52,
      panel53);                   // more flexible than a GridLayout
    panel54.setBorder(emptyBorder);
    panel54.setResizeWeight(0.6); // give more space to data bytes than text
    panel21.add(panel54, BorderLayout.CENTER);

    /* The bottom has labels for the text areas and associated buttons. */

    JPanel panel71 = new JPanel();
    panel71.setLayout(new BoxLayout(panel71, BoxLayout.X_AXIS));

    JLabel label72 = new JLabel("Hexadecimal Data Bytes");
    label72.setFont(buttonFont);
    panel71.add(label72);
    panel71.add(Box.createHorizontalStrut(20));

    clearByteButton = new JButton("Clear");
    clearByteButton.addActionListener(action);
    clearByteButton.setFont(buttonFont);
    clearByteButton.setToolTipText("Delete all hex data bytes.");
    panel71.add(clearByteButton);
    panel71.add(Box.createHorizontalStrut(20));

    copyByteButton = new JButton("Copy");
    copyByteButton.addActionListener(action);
    copyByteButton.setFont(buttonFont);
    copyByteButton.setToolTipText("Copy hex data bytes to clipboard.");
    panel71.add(copyByteButton);
    panel71.add(Box.createHorizontalStrut(40));
    panel71.add(Box.createHorizontalGlue());

    JLabel label73 = new JLabel("Text Characters (Unicode)");
    label73.setFont(buttonFont);
    panel71.add(label73);
    panel71.add(Box.createHorizontalStrut(20));

    clearCharButton = new JButton("Clear");
    clearCharButton.addActionListener(action);
    clearCharButton.setFont(buttonFont);
    clearCharButton.setToolTipText("Delete all text characters.");
    panel71.add(clearCharButton);
    panel71.add(Box.createHorizontalStrut(20));

    copyCharButton = new JButton("Copy");
    copyCharButton.addActionListener(action);
    copyCharButton.setFont(buttonFont);
    copyCharButton.setToolTipText("Copy text characters to clipboard.");
    panel71.add(copyCharButton);

    panel21.add(panel71, BorderLayout.SOUTH);

    /* Create the main window frame for this application.  We use a border
    layout to add margins around a central area for the panels above. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    JPanel panel91 = (JPanel) mainFrame.getContentPane();
    panel91.setLayout(new BorderLayout(0, 0));
    panel91.add(Box.createVerticalStrut(15), BorderLayout.NORTH);
    panel91.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel91.add(panel21, BorderLayout.CENTER);
    panel91.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
    panel91.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* Let the graphical interface run the application now. */

    convertTextToBytes();         // start by converting explanatory text

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  convertBytesToText() method

  Convert hexadecimal data bytes to text characters with the user's selected
  character set encoding.
*/
  static void convertBytesToText()
  {
    char ch;                      // one character from input string
    String code;                  // user's character set encoding
    byte[] dataBytes;             // encoded (binary) data bytes
    int dataLength;               // number of data bytes in <dataBytes>
    int digitCount;               // number of digits found in current byte
    String hexBytes;              // characters from user's hex data bytes
    int hexLength;                // number of characters in <hexBytes>
    int i;                        // index variable
    String textChars;             // new characters to user's text field
    int value;                    // one binary data byte as an integer

    /* First, convert the user's hexadecimal data bytes to binary bytes. */

    dataLength = 0;               // no binary data bytes found
    digitCount = 0;               // no digits found in current byte
    hexBytes = byteField.getSelectedText(); // first look for a selection
    if ((hexBytes == null) || (hexBytes.length() == 0)) // if no selection
      hexBytes = byteField.getText(); // get all hex data bytes
    hexLength = hexBytes.length(); // number of hex digits or spaces, etc
    dataBytes = new byte[hexLength]; // always more than what we need
    value = 0;                    // no initial value for this data byte
    for (i = 0; i < hexLength; i ++)
    {
      ch = hexBytes.charAt(i);    // get one hex digit, punctuation, other
      if ((ch >= '0') && (ch <= '9')) // decimal digit?
      {
        value = (value << 4) + (ch - '0'); // shift old left, add new digit
        digitCount ++;            // at least one digit found
      }
      else if ((ch >= 'A') && (ch <= 'F')) // uppercase hex digit?
      {
        value = (value << 4) + (ch - 'A' + 10);
        digitCount ++;
      }
      else if ((ch >= 'a') && (ch <= 'f')) // lowercase hex digit?
      {
        value = (value << 4) + (ch - 'a' + 10);
        digitCount ++;
      }
      else if (ch < 'A')          // treat many symbols as separators
      {
        if (digitCount > 0) digitCount = 2; // accept a single digit
      }
      else                        // draw the line at obviously bad input
      {
        charField.setText("Data bytes may have hexadecimal digits (0-9 A-F"
          + " a-f) and basic punctuation as separators.");
        return;                   // don't bother doing anything more
      }

      if (digitCount >= 2)        // new binary byte every two hex digits
      {
        dataBytes[dataLength ++] = (byte) value; // save one data byte
        digitCount = value = 0;   // no half data for next digit
      }
    }
    if (digitCount > 0)           // could be a trailing single digit
    {
      dataBytes[dataLength ++] = (byte) value;
    }

    /* Second, encode these binary data bytes as text characters. */

    code = (String) encodeDialog.getSelectedItem(); // get name of encoding
    try                           // name for encoding may be invalid
    {
      if (code.equals(LOCAL_ENCODING)) // use local system's encoding?
        textChars = new String(dataBytes, 0, dataLength);
      else if (code.equals(RAW_ENCODING)) // use raw bytes as characters?
        textChars = rawBytesToString(dataBytes, 0, dataLength);
      else                        // user has selected an encoding
        textChars = new String(dataBytes, 0, dataLength, code);
      charField.setText(textChars); // show user the result
      charField.select(0, 0);     // scroll home if text field too small
    }
    catch (UnsupportedEncodingException uee)
    {
      charField.setText("Unknown or unsupported character set encoding: "
        + uee.getMessage());
    }
  } // end of convertBytesToText() method


/*
  convertTextToBytes() method

  Convert text characters to hexadecimal data bytes with the user's selected
  character set encoding.
*/
  static void convertTextToBytes()
  {
    String code;                  // user's character set encoding
    byte[] dataBytes;             // encoded (binary) data bytes
    StringBuffer hexBytes;        // for building up hexadecimal bytes
    int i;                        // index variable
    String textChars;             // characters from user's text field
    int value;                    // one binary data byte as an integer

    code = (String) encodeDialog.getSelectedItem(); // get name of encoding
    textChars = charField.getSelectedText(); // first look for a selection
    if ((textChars == null) || (textChars.length() == 0)) // if no selection
      textChars = charField.getText(); // get all text characters
    try                           // name for encoding may be invalid
    {
      if (code.equals(LOCAL_ENCODING)) // use local system's encoding?
        dataBytes = textChars.getBytes();
      else if (code.equals(RAW_ENCODING)) // use raw bytes as characters?
        dataBytes = rawStringToBytes(textChars);
      else                        // user has selected an encoding
        dataBytes = textChars.getBytes(code);
      hexBytes = new StringBuffer(); // start with empty string buffer
      for (i = 0; i < dataBytes.length; i ++) // for each binary data byte
      {
        if (i > 0) hexBytes.append(' '); // insert space between hex bytes
        value = dataBytes[i] & BYTE_MASK; // get one binary data byte
        hexBytes.append(HEX_DIGITS[(value >> 4) & 0x0F]); // high-order digit
        hexBytes.append(HEX_DIGITS[value & 0x0F]); // low-order hex digit
      }
      byteField.setText(hexBytes.toString()); // show user the result
      byteField.select(0, 0);     // scroll home if text field too small
    }
    catch (UnsupportedEncodingException uee)
    {
      byteField.setText("Unknown or unsupported character set encoding: "
        + uee.getMessage());
    }
    catch (UnsupportedOperationException uoe) // ISO-2022-CN x-JISAutoDetect
    {
      byteField.setText(code + " decodes bytes as characters, but does not"
        + " encode characters as bytes.");
    }
  } // end of convertTextToBytes() method


/*
  copyToClipboard() method

  Copy selected characters from a text area to the clipboard, or copy all text
  if there is no current selection.
*/
  static void copyToClipboard(JTextArea text)
  {
    int end, start;               // index of characters in text area

    start = text.getSelectionStart(); // index of first character
    end = text.getSelectionEnd(); // index after last character
    if (start < end)              // if there is a current selection
      text.copy();                // copy selected text to clipboard
    else                          // no selection, so copy all text
    {
      text.selectAll();           // select all text (temporary)
      text.copy();                // copy all text to clipboard
      text.select(start, end);    // restore previous caret position
    }
  } // end of copyToClipboard() method


/*
  rawBytesToString() method

  Convert an array of binary data bytes to a string, mapping values from 0x00
  to 0xFF.  An old method in the String class does this, but is "deprecated"
  and liable to generate warning messages.

  This method is of questionable validity: 0x7F to 0x9F are control codes in
  the Unicode standard, not printable characters.
*/
  static String rawBytesToString( // same parameters as String constructor
    byte[] input,                 // byte array
    int offset,                   // index of starting byte
    int length)                   // number of bytes to do
  {
    StringBuffer buffer;          // faster than String for multiple appends
    int end;                      // index of ending byte
    int i;                        // index variable

    buffer = new StringBuffer();  // allocate empty string buffer for result
    end = offset + length;        // last byte we do is the one before this
    for (i = offset; i < end; i ++) // for each input byte
      buffer.append((char) (input[i] & BYTE_MASK)); // copy as a character
    return(buffer.toString());    // give caller our converted string

  } // end of rawBytesToString() method


/*
  rawStringToBytes() method

  Convert a string to an array of binary data bytes, mapping values from 0x00
  to 0xFF.  An old method in the String class does this, but is "deprecated"
  and liable to generate warning messages.

  This method is of questionable accuracy: characters pasted into a JTextArea
  may have already been interpreted according to the local system's default
  character set (encoding).
*/
  static byte[] rawStringToBytes(String input)
  {
    int i;                        // index variable
    int length;                   // size of input string in characters
    byte[] result;                // our converted result

    length = input.length();      // number of characters in caller's string
    result = new byte[length];    // same number of bytes in resulting array
    for (i = 0; i < length; i ++) // for each input character
      result[i] = (byte) (input.charAt(i) & BYTE_MASK); // copy as a byte
    return(result);               // give caller our converted bytes

  } // end of rawStringToBytes() method


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("This is a graphical application. You may give options on the command line:");
    System.err.println();
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -e# = select name of character set encoding; example: -eUTF-8");
    System.err.println("  -f# = monospaced font name for hex data bytes; example: -f\"Lucida Console\"");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main HexByteChar1 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == clearByteButton) // "Clear" button for data bytes
    {
      byteField.setText(null);    // clear text area
      byteField.requestFocusInWindow(); // give keyboard focus to empty text
    }
    else if (source == clearCharButton) // "Clear" button for text chars
    {
      charField.setText(null);
      charField.requestFocusInWindow();
    }
    else if (source == convertByteButton) // "Convert" button for data bytes
    {
      convertBytesToText();       // more work than we want to do here
      charField.requestFocusInWindow(); // give keyboard focus to result
    }
    else if (source == convertCharButton) // "Convert" button for text chars
    {
      convertTextToBytes();
      byteField.requestFocusInWindow();
    }
    else if (source == copyByteButton) // "Copy" button for data bytes
    {
      copyToClipboard(byteField); // common method for both text areas
      byteField.requestFocusInWindow(); // return keyboard focus to source
    }
    else if (source == copyCharButton) // "Copy" button for text chars
    {
      copyToClipboard(charField);
      charField.requestFocusInWindow();
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // immediate exit from GUI with no status
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of HexByteChar1 class

// ------------------------------------------------------------------------- //

/*
  HexByteChar1User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class HexByteChar1User implements ActionListener
{
  /* empty constructor */

  public HexByteChar1User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    HexByteChar1.userButton(event);
  }

} // end of HexByteChar1User class

/* Copyright (c) 2022 by Keith Fenske.  Apache License or GNU GPL. */
