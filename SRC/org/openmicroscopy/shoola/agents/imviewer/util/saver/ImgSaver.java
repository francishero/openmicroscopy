/*
 * org.openmicroscopy.shoola.agents.imviewer.util.saver.ImgSaver
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.imviewer.util.saver;



//Java imports
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JFrame;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.imviewer.ImViewerAgent;
import org.openmicroscopy.shoola.agents.imviewer.util.ImagePaintingFactory;
import org.openmicroscopy.shoola.agents.imviewer.view.ImViewer;
import org.openmicroscopy.shoola.agents.util.EditorUtil;
import org.openmicroscopy.shoola.env.log.Logger;
import org.openmicroscopy.shoola.env.ui.UserNotifier;
import org.openmicroscopy.shoola.util.filter.file.TIFFFilter;
import org.openmicroscopy.shoola.util.image.geom.Factory;
import org.openmicroscopy.shoola.util.image.io.Encoder;
import org.openmicroscopy.shoola.util.image.io.TIFFEncoder;
import org.openmicroscopy.shoola.util.image.io.WriterImage;
import org.openmicroscopy.shoola.util.ui.MessageBox;
import org.openmicroscopy.shoola.util.ui.RegExFactory;
import org.openmicroscopy.shoola.util.ui.UIUtilities;

/** 
 * A modal dialog to save the currently rendered image.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">a.falconi@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $ $Date: $)
 * </small>
 * @since OME2.2
 */
public class ImgSaver
    extends JDialog
{
    
	/** Indicates to display all possible save options. */
	public static final int 	FULL = 0;
	
	/** Indicates to display save options without lens saving options. */
	public static final int 	PARTIAL = 1;
	
	/** Indicates to display save options without lens saving options. */
	public static final int 	BASIC = 2;
	
    /** The window's title. */
    static final String 		TITLE = "Save Image";
    
    /** The title of the preview window. */
    static final String 		PREVIEW_TITLE =  "Preview image to save.";
    
    /** 
     * Indicates that the question dialog is for the <code>Preview</code>
     * dialog.
     */
    static final int			PREVIEW = 0;
    
    /** Indicates that the question dialog will save the image directly. */
    static final int			DIRECT = 1;
    
    /** The message when an image with the name and extension aleady exists. */
    private static final String	MESSAGE = "A file with the same name and \n" +
                                "extension already exists in this " +
                                "directory.\n " +
                                "Do you really want to save the image?";
    
    /** Reference to the model. */
    private ImViewer        model;
    
    /** The UI delegate. */
    private ImgSaverUI      uiDelegate;

    /** The name of the file to save. */
    private String          name;
    
    /** The file's format. */
    private String          format;
    
    /** The message displayed when the file is saved. */
    private String          saveMessage;
    
    /** The main image i.e. the one displayed in the viewer. */
    private BufferedImage   mainImage;
    
    /** 
     * The list of images composing the main image. <code>null</code> if no
     * components.
     */
    private List            imageComponents;
    
    /** The type of the image to save. */
    private int				type;
    
    /** One of the following constants: {@link #FULL}, {@link #PARTIAL}. */
    private int				savingType;
    
    /**
     * Displays the preview dialog with images depending on the 
     * saving type.
     * 
     * @param savingType The type of saving.
     */
    private void createImages(int savingType)
    {
        switch (savingType) {
            default:
            case ImgSaverUI.IMAGE:
            	type = ImgSaverUI.IMAGE;
                mainImage = model.getDisplayedImage();
                imageComponents = null;
                break;
            case ImgSaverUI.GRID_IMAGE:
            	type = ImgSaverUI.GRID_IMAGE;
                mainImage = model.getGridImage();
                imageComponents = null;
                break;
            case ImgSaverUI.IMAGE_AND_COMPONENTS:
            	type = ImgSaverUI.IMAGE_AND_COMPONENTS;
                mainImage = model.getDisplayedImage();
                imageComponents = model.getImageComponents(
                						ImViewer.RGB_MODEL);
                break;
            case ImgSaverUI.IMAGE_AND_COMPONENTS_GREY:
            	type = ImgSaverUI.IMAGE_AND_COMPONENTS;
                mainImage = model.getDisplayedImage();
                imageComponents = model.getImageComponents(
                							ImViewer.GREY_SCALE_MODEL);
                break;
            case ImgSaverUI.LENS_IMAGE:
            	type = ImgSaverUI.LENS_IMAGE;
            	mainImage = model.getZoomedLensImage();
            	imageComponents = null;
                break;
            case ImgSaverUI.LENS_IMAGE_AND_COMPONENTS:
            	type = ImgSaverUI.LENS_IMAGE_AND_COMPONENTS;
            	mainImage = model.getZoomedLensImage();
            	imageComponents = model.getLensImageComponents(
            								ImViewer.RGB_MODEL);
                break;
            case ImgSaverUI.LENS_IMAGE_AND_COMPONENTS_GREY:
            	type = ImgSaverUI.LENS_IMAGE_AND_COMPONENTS;
            	mainImage = model.getZoomedLensImage();
            	imageComponents = model.getLensImageComponents(
            								ImViewer.GREY_SCALE_MODEL);
                break;
        }
    }
    
    /**
     * Writes the image.
     * 
     * @param image The image to write to the file.
     * @param n     The name of the image.
     */
    private void writeImage(BufferedImage image, String n)
    {
        //n += "."+format;
        String extendedName = getExtendedName(n, format);
        File f = new File(extendedName);
        try {
            if (format.equals(TIFFFilter.TIF)) {
                Encoder encoder = new TIFFEncoder(Factory.createImage(image), 
                        new DataOutputStream(new FileOutputStream(f)));
                WriterImage.saveImage(encoder);
            } else WriterImage.saveImage(f, image, format);
            
            close();
        } catch (Exception e) {
        	Logger logger = ImViewerAgent.getRegistry().getLogger();
        	UserNotifier un = ImViewerAgent.getRegistry().getUserNotifier();
        	String message = e.getMessage();
            if (!f.delete())
            	message += "\nCannot delete the file.";
            logger.error(this, message);
            un.notifyError("Save image failure", "Unable to save the image", e);
        }
    }
    
    /** Sets the properties of the dialog. */
    private void setProperties()
    {
    	setTitle(TITLE);
        setModal(true);
    }
    
    /**
     * Checks and sets the saving type.
     * 
     * @param t The value to control and set.
     */
    private void checkSavingType(int t)
    {
    	switch (t) {
			case PARTIAL:
				savingType = t;
				break;
			case FULL:
			default:
				savingType = t;
				break;
		}
    }
    
    /**
     * Creates a new instance.
     * 
     * @param owner 		The owner of this dialog.
     * @param model 		Reference to the Model. 
     * 						Mustn't be <code>null</code>.
     * @param savingType 	One of the constants defined by this class.
     */
    public ImgSaver(JFrame owner, ImViewer model, int savingType)
    {
        super(owner);
        //if (model == null) throw new IllegalArgumentException("No model.");
        checkSavingType(savingType);
        this.model = model;
        setProperties();
        uiDelegate = new ImgSaverUI(this);
        pack();
    }
    
    /**
     * Adds the extension to the passed name if necessary.
     * 
     * @param name		The name to handle.
     * @param format	The selected file format.
     * @return See above.
     */
    String getExtendedName(String name, String format)
    {
    	String extension = "."+format;
    	Pattern pattern = RegExFactory.createPattern(extension);
    	String n;
    	if (RegExFactory.find(pattern, name)) {
    		n = name;
    	} else {
    		pattern = RegExFactory.createCaseInsensitivePattern(extension);
    		if (RegExFactory.find(pattern, name)) n = name;
    		else n = name + "." + format;
    	}
    	return n;
    }
    
    /**
     * Returns the name of the image.
     * 
     * @return See above.
     */
    String getPartialImageName()
    { 
    	return EditorUtil.removeFileExtension(model.getImageName());
    }
    
    /**
     * Returns the type of options available.
     * 
     * @return See above.
     */
    int getSavingType() { return savingType; }
    
    /**
     * Returns the type of image to save.
     * 
     * @return See above.
     */
    int getType() { return type; }
    
    /**
     * Sets the name of the file to save.
     * 
     * @param name The name to set.
     */
    void setFileName(String name) { this.name = name; }
    
    /**
     * Sets the format of the file.
     * 
     * @param format The file's format.
     */
    void setFileFormat(String format) { this.format = format; }
    
    /** 
     * Sets the save message.
     * 
     * @param saveMessage The message to set.
     */
    void setFileMessage(String saveMessage) { this.saveMessage = saveMessage; }
    
    /** Brings up a preview of the image or images to save. */
    void previewImage()
    { 
    	ImgSaverPreviewer preview = new ImgSaverPreviewer(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        createImages(uiDelegate.getSavingType());
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        preview.initialize();
        UIUtilities.centerAndShow(preview);
    }
    
    /**
     * Brings up on screen the dialog asking a <code>Yes/No</code>.
     * 
     * @param index One of the constants defined by this class.
     */
    void setSelection(int index)
    {
    	MessageBox dialog = new MessageBox(this, "Save Image", MESSAGE);
    	if (dialog.centerMsgBox() == MessageBox.YES_OPTION) {
    		dialog.setVisible(false);
        	switch (index) {
    			case DIRECT:
    				saveImage(true);
    				break;
    			case PREVIEW:
    				previewImage(); 
    		}
        	dialog.dispose();
    	}
    }
    
    /** Closes the window and disposes. */
    void close()
    {
    	setVisible(false);
        dispose();
    }
    
    /**
     * Creates a single image.
     * 
     * @param image		The image to create.
     * @param constrain	The constrain indicating to add the scale bar.
     * @param name		The name of the image.
     */
    private void writeSingleImage(BufferedImage image, boolean constrain, 
    							String name)
    {
    	int width = image.getWidth();
        int h = image.getHeight();
        String v = getUnitBarValue(); 
        int s = (int) model.getUnitBarSize();
        
        BufferedImage newImage = new BufferedImage(width, h, 
                					BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) newImage.getGraphics();
        ImagePaintingFactory.setGraphicRenderingSettings(g2);
        //Paint the original image.
        g2.drawImage(image, null, 0, 0); 
        
        if (constrain)
            ImagePaintingFactory.paintScaleBar(g2, width-s-10, h-10, s, v);
        writeImage(newImage, name);
    }
    
    /** 
     * Saves the displayed images. 
     * 
     * @param init	Pass <code>true</code> to initialize the images to save,
     * 				<code>false</code> otherwise.
     */
    void saveImage(boolean init)
    {
    	if (init) createImages(uiDelegate.getSavingType());
        //Builds the image to display.
        boolean unitBar = model.isUnitBar();
        String v = getUnitBarValue(); 
        int s = (int) getUnitBarSize();
        boolean constrain;
        if (imageComponents == null) {
        	constrain = unitBar && v != null && s < mainImage.getWidth() 
        				&& type == ImgSaverUI.IMAGE;
        	writeSingleImage(mainImage, constrain, name);
        } else {
        	if (mainImage == null) return;
        	Iterator i;
        	int h, w;
        	BufferedImage newImage;
        	Graphics2D g2;
        	if (uiDelegate.isSaveImagesInSeparatedFiles()) {
        		constrain = unitBar && v != null && s < mainImage.getWidth() 
							&& type == ImgSaverUI.IMAGE;
        		writeSingleImage(mainImage, constrain, name);
        		i = imageComponents.iterator();
        		int j = 0;
        		while (i.hasNext()) {
        			constrain = unitBar && v != null && 
                				type != ImgSaverUI.LENS_IMAGE_AND_COMPONENTS;
        			writeSingleImage((BufferedImage) i.next(), constrain, 
        							name+"_"+j);
        			j++;
                }
        		
        	} else {
        		int width = mainImage.getWidth();
                h = mainImage.getHeight();
                int n = imageComponents.size();
                w = width*(n+1)+ImgSaverPreviewer.SPACE*(n-1);

                newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                g2 = (Graphics2D) newImage.getGraphics();
                g2.setColor(Color.WHITE);
                ImagePaintingFactory.setGraphicRenderingSettings(g2);
                //Paint the original image.
                i = imageComponents.iterator();
                int x = 0;
                while (i.hasNext()) {
                    g2.drawImage((BufferedImage) i.next(), null, x, 0); 
                    if (unitBar && v != null && 
                        	type != ImgSaverUI.LENS_IMAGE_AND_COMPONENTS)
                        ImagePaintingFactory.paintScaleBar(g2, x+width-s-10, 
                        								h-10, s, v);
                    x += width;
                    g2.fillRect(x, 0, ImgSaverPreviewer.SPACE, h);
                    x += ImgSaverPreviewer.SPACE;
                }
                g2.drawImage(mainImage, null, x, 0); 
                if (unitBar && v != null && 
                	!(type == ImgSaverUI.LENS_IMAGE_AND_COMPONENTS ||
                	 type == ImgSaverUI.LENS_IMAGE))
                    ImagePaintingFactory.paintScaleBar(g2, x+width-s-10, h-10, 
                    									s, v);
                writeImage(newImage, name);
        	}
            
        }
        UserNotifier un = ImViewerAgent.getRegistry().getUserNotifier();
        un.notifyInfo("Image Saved", saveMessage);
        if (uiDelegate.isSetDefaultFolder())
        	UIUtilities.setDefaultFolder(uiDelegate.getCurrentDirectory());
    }

    /**
     * Returns the main image.
     * 
     * @return See above.
     */
    BufferedImage getImage() { return mainImage; }
    
    /**
     * Returns the images composing the main image i.e. one per channel
     * if two or more channels compose the main image. 
     * Returns <code>null</code> otherwise.
     * 
     * @return See above.
     */
    List getImageComponents() { return imageComponents; }

    /**
     * Returns <code>true</code> if the unit bar is painted on top of 
     * the displayed image, <code>false</code> otherwise.
     * 
     * @return See above.
     */
    boolean isUnitBar() { return model.isUnitBar(); }

    /**
     * Returns the value (with two decimals) of the unit bar or 
     * <code>null</code> if the actual value is <i>negative</i>.
     * 
     * @return See above.
     */
    String getUnitBarValue() { return model.getUnitBarValue(); }

    /**
     * Returns the size of the unit bar.
     * 
     * @return See above.
     */
    double getUnitBarSize() { return model.getUnitBarSize(); }
    
    /**
     * Returns the color of the unit bar.
     * 
     * @return See above.
     */
    Color getUnitBarColor() { return model.getUnitBarColor(); }

}
