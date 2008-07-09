/**
 * 
 */
package ui.components;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;

import tree.IAttributeSaver;
import util.ImageFactory;

public class AttributeMemoFormatEditor extends JPanel{
	/**
	 * 
	 */

	IAttributeSaver dataField;
	String attributeId;

	boolean textChanged = false;
	
	SimpleHTMLEditorPane editorPane;
	Box toolBarBox;
	Border toolBarButtonBorder;
	
	TextChangedListener textChangedListener = new TextChangedListener();
	FocusListener focusChangedListener = new FocusChangedListener();
	
	// constructor creates a new panel and adds a name and text area to it.
	public AttributeMemoFormatEditor(IAttributeSaver dataField, String attribute, String value) {
		this(dataField, attribute, attribute, value);
	}
	public AttributeMemoFormatEditor(IAttributeSaver dataField, String label, String attribute, String value) {
		
		this.dataField = dataField;
		
		this.setBorder(new EmptyBorder(3,3,3,3));
		JLabel attributeName = new JLabel(label);
		
		editorPane = new SimpleHTMLEditorPane();
		editorPane.addHtmlTagsAndSetText(value);
		attributeId = attribute;
		editorPane.addKeyListener(textChangedListener);
		editorPane.addFocusListener(focusChangedListener);
		
		int spacing = 2;
		toolBarButtonBorder = new EmptyBorder(spacing,spacing,spacing,spacing);
		toolBarBox = Box.createHorizontalBox();
		
		/*
		 * Paste button to paste clip-board text into the EditorPane. 
		 * The system Paste (Control-V) doesn't work when pasting text from
		 * a word document, perhaps because it contains unknown characters.
		 * NB. formatting is lost in this process. 
		 */
		Icon pasteIcon = ImageFactory.getInstance().getIcon(ImageFactory.PASTE_ICON);
		JButton pasteButton = new JButton(pasteIcon);
		pasteButton.setToolTipText("Paste Text");
		pasteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
		        Transferable t = Toolkit.getDefaultToolkit().
		        	getSystemClipboard().getContents(null);
		    
		        try {
		            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
		                String text = (String)t.getTransferData(DataFlavor.stringFlavor);
		                int offset = editorPane.getCaretPosition();
		                editorPane.getDocument().insertString(offset, text, null);
		            }
		        } catch (UnsupportedFlavorException ex) {
		        } catch (IOException ex) {
		        } catch (BadLocationException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}    
			}
		}
		);
		pasteButton.setBorder(toolBarButtonBorder);
		toolBarBox.add(pasteButton);
		
		
		Icon bulletPointsIcon = ImageFactory.getInstance().getIcon(ImageFactory.BULLET_POINTS_ICON); 
		JButton unorderedListButton = new JButton(bulletPointsIcon);
		unorderedListButton.setActionCommand(SimpleHTMLEditorPane.INSERT_UNORDERED_LIST_ITEM);
		unorderedListButton.addActionListener(new FontFormattingChangedListener());
		unorderedListButton.setBorder(toolBarButtonBorder);
		toolBarBox.add(unorderedListButton);
		
		Icon boldIcon = ImageFactory.getInstance().getIcon(ImageFactory.BOLD_ICON); 
		JButton boldButton = new JButton(boldIcon);
		boldButton.setFocusable(false);		// don't want to lose focus from the editorPane when clicked
		boldButton.setActionCommand(SimpleHTMLEditorPane.FONT_BOLD);
		boldButton.addActionListener(new FontFormattingChangedListener());
		boldButton.setBorder(toolBarButtonBorder);
		toolBarBox.add(boldButton);
		
		Icon underlineIcon = ImageFactory.getInstance().getIcon(ImageFactory.UNDERLINE_ICON); 
		JButton underlineButton = new JButton(underlineIcon);
		underlineButton.setFocusable(false);		// don't want to lose focus from the editorPane when clicked
		underlineButton.setActionCommand(SimpleHTMLEditorPane.FONT_UNDERLINE);
		underlineButton.addActionListener(new FontFormattingChangedListener());
		underlineButton.setBorder(toolBarButtonBorder);
		toolBarBox.add(underlineButton);
		

		
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(attributeName, BorderLayout.WEST);
		topPanel.add(toolBarBox, BorderLayout.EAST);
		
		this.setLayout(new BorderLayout());
		this.add(topPanel, BorderLayout.NORTH);
		this.add(editorPane, BorderLayout.CENTER);
	}
	public String getTextAreaText() {
		return editorPane.getTextNoBodyTagOrHtmlTag();
	}
	public void setTextAreaText(String text) {
		editorPane.addHtmlTagsAndSetText(text);
	}
	public JEditorPane getTextArea() {
		return editorPane;
	}
	public void addToToolBar(JComponent component) {
		component.setBorder(toolBarButtonBorder);
		toolBarBox.add(component);
	}
	
	public void removeFocusListener() {
		editorPane.removeFocusListener(focusChangedListener);
	}
	
	/*
	 * Listens to buttons for changing the editorPane formatting eg Underline, bold.
	 * First carries out the appropriate Action
	 * Then updates the dataField with newly modified text from the editorPane
	 */
	public class FontFormattingChangedListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			// the ActionCommand of the button corresponds to the name of the Action to be 
			// carried out by the editorPane. 
			String actionCommand = e.getActionCommand();
			
			// change the formatting of the text in the editorPane
			editorPane.getHtmlEditorKitAction(actionCommand).actionPerformed(e);
			
			// update the changes to the dataField
			setDataFieldAttribute(attributeId, getText(), true);
			textChanged = false;
		}
	}
	
	// called to update dataField with attribute
	protected void setDataFieldAttribute(String attributeName, String value, boolean notifyUndoRedo) {
		dataField.setAttribute(attributeName, value, notifyUndoRedo);
	}
	
	public class TextChangedListener implements KeyListener {
		
		public void keyTyped(KeyEvent event) {
			textChanged = true;		// some character was typed, so set this flag
		}
		public void keyPressed(KeyEvent event) {}
		public void keyReleased(KeyEvent event) {}
	
	}
	
	public class FocusChangedListener implements FocusListener {
		
		public void focusLost(FocusEvent event) {
			if (textChanged) {
				
				setDataFieldAttribute(attributeId, getText(), true);
				
				textChanged = false;
			}
		}
		public void focusGained(FocusEvent event) {}
	}
	
	// don't want to save the start and end <html> tags and <body> tags.
	public String getText() {
		return editorPane.getTextNoBodyTagOrHtmlTag().trim();
	}
}