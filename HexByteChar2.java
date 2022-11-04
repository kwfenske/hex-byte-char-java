/*
  Hexadecimal Byte Character #2 - Convert Encoded Data Bytes to Character Text
  Written by: Keith Fenske, http://kwfenske.github.io/
  Monday, 5 September 2022
  Java class name: HexByteChar2
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
  HexByteChar2 is free software and has been released under the terms and
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

      java  HexByteChar2  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u16 or -u18 is recommended for the font size.

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
  (3) JTextArea can be slow with lots of text and few line breaks (newline
      characters).  This is especially true for the hex data when formatted as
      a single line.  The problem is our lazy use of "word wrap" to fill lines
      in a text area.  KF, 2022-10-05.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support
import javax.swing.border.*;      // decorative borders

public class HexByteChar2
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
  static String byteGapString;    // separator between hex data bytes
  static int byteGroupSize;       // number of hex data bytes per group (small)
  static int byteLineSize;        // number of hex data bytes per line (bigger)
  static JTextArea charField;     // text characters displayed in Unicode
  static JButton clearByteButton, clearCharButton, convertByteButton,
    convertCharButton, copyByteButton, copyCharButton, exitButton,
    pasteByteButton, pasteCharButton, readByteButton, writeByteButton,
    zorgByteButton;               // buttons
  static JComboBox encodeDialog;  // user's choice for character set encoding
  static JFileChooser fileChooser; // asks for input and output file names
  static String groupGapString;   // separator between groups of data bytes
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
    Font byteFont;                // font for hexadecimal data bytes only
    String byteFontName;          // preferred font name for hex data bytes
    int byteFontSize;             // normal font size or chosen by user
    boolean byteWrapFlag;         // true if we wrap lines for hex data bytes
    Font commonFont;              // font for buttons, labels, status, etc
    String commonFontName;        // preferred font name for buttons, etc
    int commonFontSize;           // normal font size or chosen by user
    Border emptyBorder;           // remove borders around text areas
    String encodeName;            // select name of character set encoding
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    Font outputFont;              // font for text characters only
    String outputFontName;        // preferred font name for text characters
    int outputFontSize;           // normal font size or chosen by user
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line
    boolean zorgEnableFlag;       // true if we show the dreaded "Zorg" button

    /* Initialize variables used by both console and GUI applications. */

    byteFontName = "Lucida Console"; // many systems have this font installed
    byteFontSize = 18;            // same or smaller than common font size
    byteGapString = " ";          // default separator between hex data bytes
    byteGroupSize = 1440;         // optional grouping of hex data bytes
    byteLineSize = 1440;          // should be multiple of <byteGroupSize>
    byteWrapFlag = true;          // by default, wrap lines for hex data bytes
    commonFontName = SYSTEM_FONT; // default to normal font on local system
    commonFontSize = 18;          // preferred font size (user may change)
    encodeName = "UTF-8";         // most common character set in the world
    groupGapString = "  ";        // default separator between groups of bytes
    mainFrame = null;             // during setup, there is no GUI window
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    outputFontName = "Arial Unicode MS"; // big but may not be installed
    outputFontSize = 19;          // slight increase from common font size
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;
    zorgEnableFlag = false;       // by default, don't show the "Zorg" button

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

      else if (word.startsWith("-b") || (mswinFlag && word.startsWith("/b")))
        byteGapString = args[i].substring(2); // accept anything for separator

      else if (word.startsWith("-e") || (mswinFlag && word.startsWith("/e")))
        encodeName = args[i].substring(2); // accept anything for encoding

      else if (word.startsWith("-f") || (mswinFlag && word.startsWith("/f")))
        byteFontName = args[i].substring(2); // accept anything for font name

      else if (word.startsWith("-g") || (mswinFlag && word.startsWith("/g")))
        groupGapString = args[i].substring(2); // accept anything for separator

      else if (word.startsWith("-n") || (mswinFlag && word.startsWith("/n")))
      {
        /* This option is followed by the number of hex data bytes per line,
        which may also be a number of bytes per group and a number of groups
        per line.  The expected way to disable grouping is to set the group
        size to the same value as the line size.  The line size should always
        be a multiple of the group size. */

        Pattern pattern = Pattern.compile(
          "(\\d{1,5})|(?:\\((\\d{1,5}),(\\d{1,5})\\))");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches() == false) // bad syntax or too many digits
          byteGroupSize = byteLineSize = -1; // set result to an illegal value
        else if ((matcher.group(1) != null) && (matcher.group(1).length() > 0))
        {                         // given number of bytes per line
          byteGroupSize = byteLineSize = Integer.parseInt(matcher.group(1));
          if ((byteLineSize < 1) || (byteLineSize > 999))
            byteGroupSize = byteLineSize = -1; // flag this as an error
        }
        else                      // given bytes per group, number of groups
        {
          byteGroupSize = Integer.parseInt(matcher.group(2));
          byteLineSize = Integer.parseInt(matcher.group(3)); // groups per line
          if ((byteGroupSize < 2) || (byteGroupSize > 99) || (byteLineSize < 2)
            || (byteLineSize > 99))
          {
            byteGroupSize = byteLineSize = -1; // flag this as an error
          }
          else
            byteLineSize *= byteGroupSize; // calculate total bytes per line
        }
        if ((byteGroupSize <= 0) || (byteLineSize <= 0)) // any errors flagged?
        {
          System.err.println("Invalid number of hex data bytes per line: "
            + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        byteWrapFlag = false;     // don't wrap once user chooses a size
      }

      else if (word.startsWith("-t") || (mswinFlag && word.startsWith("/t")))
        outputFontName = args[i].substring(2); // accept anything for font name

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        try                       // try to parse remainder as unsigned integer
        {
          commonFontSize = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          commonFontSize = -1;    // set result to an illegal value
        }
        if ((commonFontSize < 10) || (commonFontSize > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        byteFontSize = (int) Math.round(1.00 * commonFontSize); // same
        outputFontSize = (int) Math.round(1.04 * commonFontSize); // increase
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\((-?\\d{1,5}),(-?\\d{1,5}),(\\d{1,5}),(\\d{1,5})\\)");
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

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // yes, maximize our main window

      else if (word.equals("-z") || (mswinFlag && word.equals("/z")))
        zorgEnableFlag = true;    // show the dreaded "Zorg" button

      else                        // parameter is not a recognized option
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }
    }

    /* Initialize shared graphical objects. */

    action = new HexByteChar2User(); // create our shared action listener
    byteFont = new Font(byteFontName, Font.PLAIN, byteFontSize); // hex data
    commonFont = new Font(commonFontName, Font.PLAIN, commonFontSize);
    emptyBorder = BorderFactory.createEmptyBorder(); // for removing borders
    fileChooser = new JFileChooser(); // create our shared file chooser
    outputFont = new Font(outputFontName, Font.PLAIN, outputFontSize);

    /* Create the graphical interface as a series of smaller panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel261, label354, etc). */

    JPanel panel21 = new JPanel(new BorderLayout(0, 0));

    /* The top has control buttons and the character set encoding.  BoxLayout
    expands JComboBox vertically to match JButton in size and base line (text).
    FlowLayout centers but does not allow JComboBox to expand horizontally. */

    JPanel panel31 = new JPanel();
    panel31.setLayout(new BoxLayout(panel31, BoxLayout.X_AXIS));

    convertByteButton = new JButton("Convert Bytes to Text");
    convertByteButton.addActionListener(action);
    convertByteButton.setFont(commonFont);
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
    encodeDialog.setFont(commonFont);
//  encodeDialog.setMnemonic(KeyEvent.VK_E); // not supported for JComboBox
//  encodeDialog.setOpaque(false); // doesn't work, would be a nice touch
    encodeDialog.setSelectedItem(encodeName); // selected item is our default
    encodeDialog.setToolTipText("Select name of character set encoding.");
//  encodeDialog.addActionListener(action); // do last so don't fire early
    panel31.add(encodeDialog);
    panel31.add(Box.createHorizontalStrut(40));

    convertCharButton = new JButton("Convert Text to Bytes");
    convertCharButton.addActionListener(action);
    convertCharButton.setFont(commonFont);
    convertCharButton.setMnemonic(KeyEvent.VK_T);
//  convertCharButton.setDisplayedMnemonicIndex(8);
    convertCharButton.setToolTipText(
      "Convert Unicode text characters to encoded bytes.");
    panel31.add(convertCharButton);
    panel31.add(Box.createHorizontalStrut(40));

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    exitButton.setFont(commonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel31.add(exitButton);

    JPanel panel33 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    panel33.add(panel31);         // center without horizontal expansion
    JPanel panel34 = new JPanel(new BorderLayout(0, 0));
    panel34.add(panel33, BorderLayout.CENTER);
    panel34.add(Box.createVerticalStrut(11), BorderLayout.SOUTH);
    panel21.add(panel34, BorderLayout.NORTH);

    /* The middle has text areas for hex data bytes and text characters.  The
    number of JTextArea columns sets an initial ratio for the widths, and will
    expand later when the layout is validated.  Note that the two JTextArea
    have different fonts and hence our column widths are approximate. */

    byteField = new JTextArea(26, 5);
    byteField.setEditable(true);
    byteField.setFont(byteFont);
    byteField.setLineWrap(byteWrapFlag); // don't wrap if user set line size
    byteField.setMargin(new Insets(4, 7, 4, 7)); // top, left, bottom, right
    byteField.setWrapStyleWord(true);
    JScrollPane panel52 = new JScrollPane(byteField);
    panel52.setBorder(emptyBorder); // no border necessary here

    charField = new JTextArea(16, 6);
    charField.setEditable(true);
    charField.setFont(outputFont);
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
    panel71.setLayout(new BoxLayout(panel71, BoxLayout.Y_AXIS));
    panel71.add(Box.createVerticalStrut(8));

    JPanel panel72 = new JPanel();
    panel72.setLayout(new BoxLayout(panel72, BoxLayout.X_AXIS));
    JLabel label73 = new JLabel("Hexadecimal Data Bytes");
    label73.setFont(commonFont);
    panel72.add(label73);
    panel72.add(Box.createHorizontalStrut(40));
    panel72.add(Box.createHorizontalGlue());
    JLabel label74 = new JLabel("Text Characters (Unicode)");
    label74.setFont(commonFont);
    panel72.add(label74);
    panel71.add(panel72);
    panel71.add(Box.createVerticalStrut(6));

    JPanel panel75 = new JPanel();
    panel75.setLayout(new BoxLayout(panel75, BoxLayout.X_AXIS));

    clearByteButton = new JButton("Clear");
    clearByteButton.addActionListener(action);
    clearByteButton.setFont(commonFont);
    clearByteButton.setToolTipText("Delete all hex data bytes.");
    panel75.add(clearByteButton);
    panel75.add(Box.createHorizontalStrut(10));

    copyByteButton = new JButton("Copy");
    copyByteButton.addActionListener(action);
    copyByteButton.setFont(commonFont);
    copyByteButton.setToolTipText("Copy hex data bytes to clipboard.");
    panel75.add(copyByteButton);
    panel75.add(Box.createHorizontalStrut(10));

    pasteByteButton = new JButton("Paste");
    pasteByteButton.addActionListener(action);
    pasteByteButton.setFont(commonFont);
    pasteByteButton.setToolTipText("Paste clipboard as hex data bytes.");
    panel75.add(pasteByteButton);
    panel75.add(Box.createHorizontalStrut(10));

    readByteButton = new JButton("Read");
    readByteButton.addActionListener(action);
    readByteButton.setFont(commonFont);
    readByteButton.setToolTipText("Read data bytes from file.");
    panel75.add(readByteButton);
    panel75.add(Box.createHorizontalStrut(10));

    writeByteButton = new JButton("Write");
    writeByteButton.addActionListener(action);
    writeByteButton.setFont(commonFont);
    writeByteButton.setToolTipText("Write data bytes to file.");
    panel75.add(writeByteButton);
    if (zorgEnableFlag) panel75.add(Box.createHorizontalStrut(10));

    zorgByteButton = new JButton("Zorg");
    zorgByteButton.addActionListener(action);
    zorgByteButton.setFont(commonFont);
    zorgByteButton.setToolTipText("Jean-Baptiste Emanuel Zorg");
    if (zorgEnableFlag) panel75.add(zorgByteButton);
    panel75.add(Box.createHorizontalStrut(40));
    panel75.add(Box.createHorizontalGlue());

    clearCharButton = new JButton("Clear");
    clearCharButton.addActionListener(action);
    clearCharButton.setFont(commonFont);
    clearCharButton.setToolTipText("Delete all text characters.");
    panel75.add(clearCharButton);
    panel75.add(Box.createHorizontalStrut(10));

    copyCharButton = new JButton("Copy");
    copyCharButton.addActionListener(action);
    copyCharButton.setFont(commonFont);
    copyCharButton.setToolTipText("Copy text characters to clipboard.");
    panel75.add(copyCharButton);
    panel75.add(Box.createHorizontalStrut(10));

    pasteCharButton = new JButton("Paste");
    pasteCharButton.addActionListener(action);
    pasteCharButton.setFont(commonFont);
    pasteCharButton.setToolTipText("Paste clipboard as text characters.");
    panel75.add(pasteCharButton);

    panel71.add(panel75);
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
    panel91.add(Box.createVerticalStrut(11), BorderLayout.SOUTH);

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
  clearTextArea() method

  Clear a text area by setting it to an empty string.  This is at the request
  of the user, so the text area also receives the keyboard focus.
*/
  static void clearTextArea(JTextArea text)
  {
    text.setText(null);           // null value or zero-length string
    text.requestFocusInWindow();  // give keyboard focus to result
  }


/*
  clipboardCopy() method

  Copy selected characters from a text area to the clipboard, or copy all text
  if there is no current selection.
*/
  static void clipboardCopy(JTextArea text)
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
    text.requestFocusInWindow();  // give keyboard focus to source

  } // end of clipboardCopy() method


/*
  clipboardPaste() method

  Copy characters from the clipboard to a text area.  We don't change the
  behavior of the current selection, if any.  This does nothing more than
  Control-V on most systems.  Our "Paste" buttons aren't really necessary.
*/
  static void clipboardPaste(JTextArea text)
  {
//  text.selectAll();             // if "Paste" button replaces all text
    text.paste();                 // copy clipboard to text area
    text.requestFocusInWindow();  // give keyboard focus to result
  }


/*
  convertBytesToText() method

  Convert hexadecimal data bytes to text characters with the user's selected
  character set encoding.
*/
  static void convertBytesToText()
  {
    String code;                  // user's character set encoding
    byte[] dataBytes;             // encoded (binary) data bytes
    String hexChars;              // characters from user's hex data bytes
    String textChars;             // new characters to user's text area

    /* First, convert the user's hexadecimal data bytes to binary bytes. */

    hexChars = byteField.getSelectedText(); // first look for a selection
    if ((hexChars == null) || (hexChars.length() == 0)) // if no selection
      hexChars = byteField.getText(); // get all hex data bytes
    dataBytes = hexDataToBytes(hexChars, false); // try convert to real bytes
    if (dataBytes == null)        // was there an error, did we tell user?
      return;                     // yes, do nothing more

    /* Second, encode these binary data bytes as text characters. */

    code = (String) encodeDialog.getSelectedItem(); // get name of encoding
    try                           // name for encoding may be invalid
    {
      if (code.equals(LOCAL_ENCODING)) // use local system's encoding?
        textChars = new String(dataBytes);
      else if (code.equals(RAW_ENCODING)) // use raw bytes as characters?
        textChars = rawBytesToString(dataBytes);
      else                        // user has selected an encoding
        textChars = new String(dataBytes, code);

      charField.setText(textChars); // replace all text
      charField.select(0, 0);     // scroll home, default is end of text
      charField.requestFocusInWindow(); // give keyboard focus to result
    }
    catch (UnsupportedEncodingException uee)
    {
      JOptionPane.showMessageDialog(mainFrame,
        ("Unknown or unsupported character set encoding:\n"
        + uee.getMessage()));
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
    String textChars;             // characters from user's text area

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

      byteField.setText(hexDataFromBytes(dataBytes)); // replace all text
      byteField.select(0, 0);     // scroll home, default is end of text
      byteField.requestFocusInWindow(); // give keyboard focus to result
    }
    catch (UnsupportedEncodingException uee)
    {
      JOptionPane.showMessageDialog(mainFrame,
        ("Unknown or unsupported character set encoding:\n"
        + uee.getMessage()));
    }
    catch (UnsupportedOperationException uoe) // ISO-2022-CN x-JISAutoDetect
    {
      JOptionPane.showMessageDialog(mainFrame, (code
        + " decodes bytes as characters,\nbut does not encode characters as bytes."));
    }
  } // end of convertTextToBytes() method


/*
  doReadButton() method

  Read a file as bytes, convert those bytes to hexadecimal, and put this into
  the text area for hex data bytes.  We don't update the the other text area
  for text characters.  (The user can do that.)
*/
  static void doReadButton()
  {
    byte[] fileBuffer;            // one big buffer to hold entire file
    int fileLength;               // size of user's file in bytes
    FileInputStream fileStream;   // read bytes directly, no buffering
    int i;                        // index variable
    File userFile;                // Java File object to read

    /* Ask the user for an input file to read. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Read File as Bytes...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    userFile = fileChooser.getSelectedFile(); // get file selected by user

    /* See if we can read from the user's chosen file. */

    if ((userFile.isFile() == false) || (userFile.canRead() == false))
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " does not exist or can't be read."));
      return;
    }

    /* Check size of user's file and try to read all data bytes at once. */

    fileLength = (int) Math.min(userFile.length(), Integer.MAX_VALUE);
    if (fileLength < 1)           // can't do much with less than one byte
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is empty (zero bytes)."));
      return;
    }
    try                           // not all I/O goes as planned
    {
      fileBuffer = new byte[fileLength]; // one big buffer for entire file
      fileStream = new FileInputStream(userFile); // read as raw data bytes
      i = fileStream.read(fileBuffer); // try to read entire file
      fileStream.close();         // don't need this file anymore
      if (fileLength != i)        // did we get exactly what we expected?
      {
        JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
          + " size is " + fileLength + " bytes but read length was " + i
          + " bytes."));
        return;
      }

      /* Convert file binary bytes to hex data bytes, and put into the text
      area for hex data bytes. */

      byteField.setText(hexDataFromBytes(fileBuffer)); // replace all text
      byteField.select(0, 0);     // scroll home, default is end of text
      byteField.requestFocusInWindow(); // give keyboard focus to result
    }
    catch (IOException ioe)
    {
      JOptionPane.showMessageDialog(mainFrame, ("Can't read file "
        + userFile.getName() + "\n" + ioe.getMessage()));
      return;
    }
    catch (OutOfMemoryError oome)
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName() + " has "
        + fileLength + " bytes,\nwhich is too big for this program."));
      return;
    }

//  JOptionPane.showMessageDialog(mainFrame, (fileLength + " bytes read."));

  } // end of doReadButton() method


/*
  doWriteButton() method

  Convert hex data bytes to real binary bytes and write those bytes to a file.
*/
  static void doWriteButton()
  {
    byte[] dataBytes;             // encoded (binary) data bytes
    FileOutputStream fileStream;  // write bytes directly, no buffering
    String hexChars;              // characters from user's hex data bytes
    File userFile;                // Java File object to write

    /* Convert user's hexadecimal data bytes to binary bytes. */

    hexChars = byteField.getSelectedText(); // first look for a selection
    if ((hexChars == null) || (hexChars.length() == 0)) // if no selection
      hexChars = byteField.getText(); // get all hex data bytes
    dataBytes = hexDataToBytes(hexChars, false); // try convert to real bytes
    if (dataBytes == null)        // was there an error, did we tell user?
      return;                     // yes, do nothing more
    else if (dataBytes.length == 0) // don't create an empty file
    {
      JOptionPane.showMessageDialog(mainFrame,
        "There are no hex data bytes to write.");
      return;
    }

    /* Ask the user for an output file to write. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Write File as Bytes...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    userFile = fileChooser.getSelectedFile(); // get file selected by user

    /* See if we can write to the user's chosen file. */

    if (userFile.isDirectory())   // can't write to directories or folders
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a directory or folder.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isHidden()) // won't write to hidden (protected) files
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a hidden or protected file.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isFile() == false) // if file doesn't exist
    {
      /* Maybe we can create a new file by this name.  Do nothing here. */
    }
    else if (userFile.canWrite() == false) // file exists, but is read-only
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is locked or write protected.\nCan't write to this file."));
      return;
    }
    else if (JOptionPane.showConfirmDialog(mainFrame, (userFile.getName()
      + " already exists.\nDo you want to replace this with a new file?"))
      != JOptionPane.YES_OPTION)
    {
      return;                     // user cancelled file replacement dialog
    }

    /* Write real binary bytes to the user's file. */

    try                           // not all I/O goes as planned
    {
      fileStream = new FileOutputStream(userFile); // write as raw data bytes
      fileStream.write(dataBytes); // try to write entire file
      fileStream.close();         // don't need this file anymore
      byteField.requestFocusInWindow(); // give keyboard focus to source
    }
    catch (IOException ioe)
    {
      JOptionPane.showMessageDialog(mainFrame, ("Can't write file "
        + userFile.getName() + "\n" + ioe.getMessage()));
      return;
    }

//  JOptionPane.showMessageDialog(mainFrame, (dataBytes.length
//    + " bytes written."));

  } // end of doWriteButton() method


/*
  doZorgButton() method

  Clean up the hex data bytes.  Convert to binary bytes while ignoring errors.
  Convert back again to hex.  Compare to see if something has changed.  Nothing
  is accomplished most of the time, and this button is usually disabled.

  Named after the character "Jean-Baptiste Emanuel Zorg" (or just "Zorg") in
  the movie "The Fifth Element" (1997), played by Gary Oldman, who didn't like
  his own performance but is otherwise terrific in the role.
*/
  static void doZorgButton()
  {
    byte[] dataBytes;             // encoded (binary) data bytes
    String newHex;                // updated hex data bytes
    String oldHex;                // original hex data bytes

    oldHex = byteField.getText(); // get all current hex data bytes
    dataBytes = hexDataToBytes(oldHex, true); // force convert to real bytes
    newHex = hexDataFromBytes(dataBytes); // convert back to hex data bytes
    if (newHex.equals(oldHex))    // has anything really changed?
      JOptionPane.showMessageDialog(mainFrame,
        "Zorg accomplishes nothing despite great efforts.");
    else
    {
      byteField.setText(newHex);  // replace all previous hex data bytes
      byteField.select(0, 0);     // scroll home, default is end of text
    }
    byteField.requestFocusInWindow(); // give keyboard focus to result

  } // end of doZorgButton() method


/*
  hexDataFromBytes() method

  Convert real binary bytes to hexadecimal characters representing those bytes.
  We insert a space to separate bytes, with an occasional newline character (to
  help word wrap in JTextArea).  No errors are detected by this method.

  The number of bytes between newline characters may be almost anything up to
  about 128 KB, after which the speed of JTextArea starts to deteriorate.  The
  difference can be hours to display instead of seconds!  The current number
  was chosen to be a multiple of common sizes and will disguise some newlines.
*/
  static String hexDataFromBytes(byte[] input)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    int i;                        // index variable
    int value;                    // one binary data byte as an integer

    buffer = new StringBuffer();  // start with empty string buffer
    for (i = 0; i < input.length; i ++) // for each input byte
    {
      if (i == 0) { /* do nothing: no space at beginning */ }
      else if ((i % byteLineSize) == 0) buffer.append('\n'); // start line
      else if ((i % byteGroupSize) == 0) buffer.append(groupGapString);
      else buffer.append(byteGapString); // usually a space between bytes
      value = input[i] & BYTE_MASK; // get one binary data byte
      buffer.append(HEX_DIGITS[(value >> 4) & 0x0F]); // high-order digit
      buffer.append(HEX_DIGITS[value & 0x0F]); // low-order hex digit
    }
    return(buffer.toString());    // give caller our converted string

  } // end of hexDataFromBytes() method


/*
  hexDataToBytes() method

  Convert hexadecimal characters representing data bytes to real binary bytes.
  Return <null> if the input has errors.  (A pop-up message will be produced.)
*/
  static byte[] hexDataToBytes(String input, boolean ignoreErrorsFlag)
  {
    char ch;                      // one character from input string
    byte[] dataBytes;             // encoded (binary) data bytes
    int dataLength;               // number of data bytes in <dataBytes>
    int digitCount;               // number of digits found in current byte
    int i;                        // index variable
    int inputLength;              // number of characters in <input>
    byte[] result;                // cleaned up result with correct length
    int value;                    // one binary data byte as an integer

    dataLength = 0;               // no binary data bytes found
    digitCount = 0;               // no digits found in current byte
    inputLength = input.length(); // number of hex digits or spaces, etc
    dataBytes = new byte[inputLength]; // always more than what we need
    value = 0;                    // no initial value for this data byte
    for (i = 0; i < inputLength; i ++)
    {
      ch = input.charAt(i);       // get one hex digit, punctuation, other
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
      else if (ignoreErrorsFlag   // if we ignore all other characters
        || ((ch >= 0x00) && (ch <= 0x2F)) // ASCII Unicode punctuation
        || ((ch >= 0x3A) && (ch <= 0x40))
        || ((ch >= 0x5B) && (ch <= 0x60))
        || ((ch >= 0x7B) && (ch <= 0x7F))) // safe up to 0xBF
      {
        if (digitCount > 0) digitCount = 2; // accept a single digit
      }
      else                        // draw the line at obviously bad input
      {
        JOptionPane.showMessageDialog(mainFrame,
          ("Data bytes may have hexadecimal digits (0-9 A-F a-f)\nand basic punctuation as separators."));
        return(null);             // don't bother doing anything more
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

    result = new byte[dataLength]; // truncate array to correct length
    for (i = 0; i < dataLength; i ++)
      result[i] = dataBytes[i];
    return(result);               // give caller correct byte array

  } // end of hexDataToBytes() method


/*
  rawBytesToString() method

  Convert an array of binary data bytes to a string, mapping values from 0x00
  to 0xFF.  An old method in the String class does this, but is "deprecated"
  and liable to generate warning messages.

  This method is of questionable validity: 0x7F to 0x9F are control codes in
  the Unicode standard, not printable characters.
*/
  static String rawBytesToString(byte[] input)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    int i;                        // index variable

    buffer = new StringBuffer();  // allocate empty string buffer for result
    for (i = 0; i < input.length; i ++) // for each input byte
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
    System.err.println("  -f# = font name for hex data bytes; example: -f\"Lucida Console\"");
    System.err.println("  -n# = number of hex data bytes per line (1-999), no wrap; example: -n12");
    System.err.println("  -n(#,#) = number of hex data bytes per group (2-99) and number of groups per");
    System.err.println("      line (2-99), no wrap; example: -n(4,3)");
    System.err.println("  -t# = font name for text characters; example: -tVerdana");
    System.err.println("  -u# = font size for buttons, dialogs, etc; example: -u16");
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
  buttons, in the context of the main HexByteChar2 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == clearByteButton) // "Clear" button for data bytes
    {
      clearTextArea(byteField);   // clear this text area
    }
    else if (source == clearCharButton) // "Clear" button for text chars
    {
      clearTextArea(charField);
    }
    else if (source == convertByteButton) // "Convert" button for data bytes
    {
      convertBytesToText();       // convert data bytes to text chars
    }
    else if (source == convertCharButton) // "Convert" button for text chars
    {
      convertTextToBytes();       // convert text chars to data bytes
    }
    else if (source == copyByteButton) // "Copy" button for data bytes
    {
      clipboardCopy(byteField);   // copy from this text area
    }
    else if (source == copyCharButton) // "Copy" button for text chars
    {
      clipboardCopy(charField);
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // immediate exit from GUI with no status
    }
    else if (source == pasteByteButton) // "Paste" button for data bytes
    {
      clipboardPaste(byteField);  // paste to this text area
    }
    else if (source == pasteCharButton) // "Paste" button for text chars
    {
      clipboardPaste(charField);
    }
    else if (source == readByteButton) // "Read" button for data bytes
    {
      doReadButton();
    }
    else if (source == writeByteButton) // "Write" button for data bytes
    {
      doWriteButton();
    }
    else if (source == zorgByteButton) // "Zorg" button for data bytes
    {
      doZorgButton();             // but the question is, which Zorg or Zorgs?
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of HexByteChar2 class

// ------------------------------------------------------------------------- //

/*
  HexByteChar2User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class HexByteChar2User implements ActionListener
{
  /* empty constructor */

  public HexByteChar2User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    HexByteChar2.userButton(event);
  }

} // end of HexByteChar2User class

/* Copyright (c) 2022 by Keith Fenske.  Apache License or GNU GPL. */
