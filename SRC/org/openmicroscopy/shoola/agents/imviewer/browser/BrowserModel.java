/*
 * org.openmicroscopy.shoola.agents.imviewer.browser.BrowserModel
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

package org.openmicroscopy.shoola.agents.imviewer.browser;


//Java imports
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.imviewer.IconManager;
import org.openmicroscopy.shoola.agents.imviewer.ImViewerAgent;
import org.openmicroscopy.shoola.agents.imviewer.actions.UnitBarSizeAction;
import org.openmicroscopy.shoola.agents.imviewer.actions.ZoomAction;
import org.openmicroscopy.shoola.agents.imviewer.actions.ZoomCmd;
import org.openmicroscopy.shoola.agents.imviewer.actions.ZoomGridAction;
import org.openmicroscopy.shoola.agents.imviewer.util.ImagePaintingFactory;
import org.openmicroscopy.shoola.agents.imviewer.view.ImViewer;
import org.openmicroscopy.shoola.agents.imviewer.view.ViewerPreferences;
import org.openmicroscopy.shoola.env.ui.UserNotifier;
import org.openmicroscopy.shoola.util.image.geom.Factory;
import org.openmicroscopy.shoola.util.ui.UIUtilities;

import pojos.ChannelData;
import pojos.ImageData;

/** 
 * The Model component in the <code>Browser</code> MVC triad.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">a.falconi@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $ $Date: $)
 * </small>
 * @since OME2.2
 */
class BrowserModel
{
	
	/** The text above the combined image. */
	private static final String	COMBINED = "Combined";
	
	/** Channel prefix. */
	private static final String PREFIX ="w=";
	
	/** Gap between the images. */
	static final int 			GAP = 2;
	
    /** Reference to the component that embeds this model. */ 
    private Browser         	component;
    
    /** The original image. */
    private BufferedImage   	renderedImage;
    
    /**
     * The image painted on screen.
     * This image may have been transformed i.e. zoomed, sharpened etc.
     */
    private BufferedImage   	displayedImage;
    
    /** The projected image. */
    private BufferedImage		projectedImage;
    
    /** The projected image. */
    private BufferedImage		displayedProjectedImage;
   
    /** A smaller version (default 50%) of the original image. */
    private BufferedImage		combinedImage;

    /** The zoom factor. */
    private double          	zoomFactor;
    
    /** Reference to the {@link ImViewer}. */
    private ImViewer        	parent;
    
    /** 
     * Flag to indicate if the unit bar is painted or not on top of the
     * displayed image.
     */
    private boolean         	unitBar;
    
    /** The value of the unit bar in microns. */
    private double          	unitInMicrons;
    
    /** The default color of the unit bar. */
    private Color				unitBarColor;
    
    /** The bacground color of the canvas. */
    private Color				backgroundColor;
    
    /** Unloaded image data. */
    private ImageData			data;

    /** Collection of images composing the grid. */
    private List<BufferedImage>	originalGridImages;
    
    /** Collection of retrieved images composing the grid. */
    private List<BufferedImage>	gridImages;

    /** Collection of images composing the grid. */
    private List<SplitImage>	splitImages;
    
    /** The magnification factor used to render the annotate image. */
   private double 				ratio;
    
    /** The magnification factor used to render the grid image. */
    private double				gridRatio;
    
    /** Flag indicating to initialize {@link #ratio} and {@link #unitBar}. */
    private boolean				init;
    
    /**
     * Creates a buffered image.
     * 
     * @param buf		The buffer hosting the data.
     * @param sizeX		The image's width.
     * @param sizeY		The image's height.
     * @param redMask	The mask applied on the red component.
     * @param greenMask	The mask applied on the green component.
     * @param blueMask	The mask applied on the blue component.
     * @return See above.
     */
    private BufferedImage createBandImage(DataBuffer buf, int sizeX, int sizeY, 
    							int redMask, int greenMask, int blueMask)
    {
    	int[] masks = {redMask, greenMask, blueMask};
    	
    	switch (buf.getDataType()) {
			case DataBuffer.TYPE_BYTE:
				DataBufferByte bufferByte = (DataBufferByte) buf;
	            byte[] values = bufferByte.getData();
	            int i = 0, j = 0, l = values.length/3;
	    		int[] buffer = new int[l];
	    		while (i<l)
	        		buffer[i++] = (values[j++]) | (values[j++]<<8) | 
	        						(values[j++]<<16);
				return Factory.createImage(buffer, 24, masks, sizeX, sizeY);
			case DataBuffer.TYPE_INT:
				return Factory.createImage(buf, 32, masks, sizeX, sizeY);
				 
		}
    	return null;
    }
    
    
    /**
     * Returns <code>true</code> if the channel is mapped to <code>Red</code>,
     * <code>Green</code> or <code>Blue</code>, <code>false</code> otherwise.
     * 
     * @param index The index of the channel.
     * @return See above.
     */
    private boolean isChannelRGB(int index)
    {
    	if (parent.isChannelRed(index)) return true;
		if (parent.isChannelGreen(index)) return true; 
		return parent.isChannelBlue(index);
    }
    
    /**
     * Returns <code>true</code> if the active channels are mapped
     * to <code>Red</code>, <code>Green</code> or <code>Blue</code>,
     * <code>false</code> otherwise or if the number of active channels is 0
     * or greater than 3.
     * 
     * @param channels The collection of channels to handle.
     * @return See above.
     */
    private boolean isImageRGB(List channels)
    {
    	if (channels == null) return false;
    	int n = channels.size();
    	if (n == 0 || n > 3) return false;
    	List<Boolean> rgb = new ArrayList<Boolean>();
    	int index;
    	Iterator i;
    	i = channels.iterator();
		while (i.hasNext()) {
			index = (Integer) i.next();
			if (isChannelRGB(index)) rgb.add(true);
		}
		return (n == rgb.size());
    }
    
    /** 
     * Creates the images composing the grid when the color model
     * is <code>GreyScale</code>.
     */
    private void createGridImagesForGreyScale()
    {
    	int maxC = parent.getMaxC();
    	List l = parent.getActiveChannelsInGrid();
		int n = l.size();	
		
		switch (n) {
			case 0:
				for (int i = 0; i < maxC; i++) 
					gridImages.add(null);
				combinedImage = null;
				break;
			case 1:
			case 2:
			case 3:
				if (isImageRGB(l)) {
					BufferedImage  image = parent.getCombinedGridImage();
					if (image == null) {
						for (int i = 0; i < maxC; i++) 
							gridImages.add(null);
						break;
					}
					combinedImage = Factory.magnifyImage(gridRatio, image);
					
					int w = combinedImage.getWidth();
		        	int h = combinedImage.getHeight();
		        	
		        	DataBuffer buf = 
		        		combinedImage.getRaster().getDataBuffer();
		        	List<ChannelData> list = parent.getSortedChannelData();
		        	Iterator<ChannelData> i = list.iterator();
		        	int index;
		        	while (i.hasNext()) {
						index = i.next().getIndex();
						if (l.contains(index)) {
							if (parent.isChannelRed(index)) { 
								gridImages.add(createBandImage(buf, w, h, 
										Factory.RED_MASK, Factory.RED_MASK, 
										Factory.RED_MASK));
							} else if (parent.isChannelGreen(index)) {
								gridImages.add(createBandImage(buf, w, h,
										Factory.GREEN_MASK, Factory.GREEN_MASK, 
										Factory.GREEN_MASK));
							} else if (parent.isChannelBlue(index)) {
								gridImages.add(createBandImage(buf, w, h, 
										Factory.BLUE_MASK, Factory.BLUE_MASK,
										Factory.BLUE_MASK));
							}
						} else {
							gridImages.add(null);
						}
					}
		        	/*
		    		for (int i = 0; i < maxC; i++) {
						if (l.contains(i)) {
							if (parent.isChannelRed(i)) { 
								gridImages.add(createBandImage(buf, w, h, 
										Factory.RED_MASK, Factory.RED_MASK, 
										Factory.RED_MASK));
							} else if (parent.isChannelGreen(i)) {
								gridImages.add(createBandImage(buf, w, h,
										Factory.GREEN_MASK, Factory.GREEN_MASK, 
										Factory.GREEN_MASK));
							} else if (parent.isChannelBlue(i)) {
								gridImages.add(createBandImage(buf, w, h, 
										Factory.BLUE_MASK, Factory.BLUE_MASK,
										Factory.BLUE_MASK));
							}
						} else {
							gridImages.add(null);
						}
					}
					*/
				} else {
					retrieveGridImagesForGreyScale(l);
				}
				break;
				
			default:
				retrieveGridImagesForGreyScale(l);
		}
    }
    
    /**
     * Retrieves the images composing the grid when the color model
     * is <code>GreyScale</code> and when the channels are not mapped to
     * red, green or blue i.e. a channel mapped to yellow.
     * 
     * @param channels Collection of active channels in the grid.
     */
    private void retrieveGridImagesForGreyScale(List channels)
    {
    	List<BufferedImage> images = parent.getGridImages();
    	if (images != null) {
    		int last = images.size()-1;
    		combinedImage = Factory.magnifyImage(gridRatio, 
								images.get(last));
    		images.remove(last);
    		Iterator i = images.iterator();
        	while (i.hasNext()) {
        		gridImages.add(Factory.magnifyImage(gridRatio, 
        					(BufferedImage) i.next()));
    		}
        	if (originalGridImages.size() == 0 && !isImageRGB(channels)) {
        		i = images.iterator();
	        	while (i.hasNext()) {
	        		originalGridImages.add((BufferedImage) i.next());
	    		}
        	}
    	}
    }
    
    /**
     * Retrieves the images composing the grid when the color model
     * is <code>RBG</code> and when the channels are not mapped to
     * red, green or blue i.e. a channel mapped to yellow.
     */
    private void retrieveGridImages()
    {
    	List<BufferedImage> images = parent.getGridImages();
    	if (images != null) {
    		Iterator i = images.iterator();
        	while (i.hasNext()) {
        		gridImages.add(Factory.magnifyImage(gridRatio, 
        					(BufferedImage) i.next()));
    		}
        	if (originalGridImages.size() == 0) {
        		i = images.iterator();
	        	while (i.hasNext()) 
	        		originalGridImages.add((BufferedImage) i.next());
        	}
        	combinedImage = Factory.magnifyImage(gridRatio, renderedImage);
    	}
    }

    /** Creates the images composing the grid. */
    private void createGridImages()
    {
    	if (originalGridImages == null)
    		originalGridImages = new ArrayList<BufferedImage>();
    	gridImages.clear();
    	if (parent.getColorModel().equals(ImViewer.GREY_SCALE_MODEL)) {
    		createGridImagesForGreyScale();
    		return;
    	}
    	
    	List l = parent.getActiveChannels();
    	int maxC = parent.getMaxC();
    	switch (l.size()) {
			case 0:
				for (int i = 0; i < maxC; i++) 
					gridImages.add(null);
				break;
			case 1:
			case 2:
			case 3:
				if (isImageRGB(l)) {
					if (combinedImage == null) 
						combinedImage = Factory.magnifyImage(gridRatio, 
								renderedImage);
					int w = combinedImage.getWidth();
		        	int h = combinedImage.getHeight();
		        	DataBuffer buf = combinedImage.getRaster().getDataBuffer();
		        	List<ChannelData> list = parent.getSortedChannelData();
		        	Iterator<ChannelData> i = list.iterator();
		        	int index;
		        	while (i.hasNext()) {
						index = i.next().getIndex();
						if (parent.isChannelActive(index)) {
							if (parent.isChannelRed(index)) { 
								gridImages.add(createBandImage(buf, w, h, 
										Factory.RED_MASK, Factory.BLANK_MASK,
										Factory.BLANK_MASK));
							} else if (parent.isChannelGreen(index)) {
								gridImages.add(createBandImage(buf, w, h,
										Factory.BLANK_MASK, Factory.GREEN_MASK, 
										Factory.BLANK_MASK));
							} else if (parent.isChannelBlue(index)) {
								gridImages.add(createBandImage(buf, w, h, 
										Factory.BLANK_MASK, Factory.BLANK_MASK, 
										Factory.BLUE_MASK));
							}
						} else {
							gridImages.add(null);
						}
					}
		        	/*
		    		for (int i = 0; i < maxC; i++) {
						if (parent.isChannelActive(i)) {
							if (parent.isChannelRed(i)) { 
								gridImages.add(createBandImage(buf, w, h, 
										Factory.RED_MASK, Factory.BLANK_MASK,
										Factory.BLANK_MASK));
							} else if (parent.isChannelGreen(i)) {
								gridImages.add(createBandImage(buf, w, h,
										Factory.BLANK_MASK, Factory.GREEN_MASK, 
										Factory.BLANK_MASK));
							} else if (parent.isChannelBlue(i)) {
								gridImages.add(createBandImage(buf, w, h, 
										Factory.BLANK_MASK, Factory.BLANK_MASK, 
										Factory.BLUE_MASK));
							}
						} else {
							gridImages.add(null);
						}
					}
					*/
		    		
				} else {
					retrieveGridImages();
				}
				break;
			default:
				retrieveGridImages();
    	}
    }
    
    /** 
     * Creates a new instance.
     * 
     * @param parent    The parent of this component.
     *                  Mustn't be <code>null</code>.
     * @param imageID	The id of the image.
     * @param pref		The preferences for the viewer.
     */
    BrowserModel(ImViewer parent, long imageID, ViewerPreferences pref)
    {
        if (parent == null) throw new IllegalArgumentException("No parent.");
        //unloaded image data
        data = new ImageData();
        data.setId(imageID);
        this.parent = parent;
        unitBar = true;
        ratio = ZoomGridAction.DEFAULT_ZOOM_FACTOR;
        gridRatio = ZoomGridAction.DEFAULT_ZOOM_FACTOR;
        init = true;
        unitInMicrons = UnitBarSizeAction.getDefaultValue(); // size microns.
        unitBarColor = ImagePaintingFactory.UNIT_BAR_COLOR;
        backgroundColor = ImagePaintingFactory.DEFAULT_BACKGROUND;
        gridImages = new ArrayList<BufferedImage>();
        zoomFactor = ZoomAction.DEFAULT_ZOOM_FACTOR;
        if (pref != null) {
        	if (pref.getBackgroundColor() != null)
        		backgroundColor = pref.getBackgroundColor();
        	if (pref.getScaleBarColor() != null)
        		unitBarColor = pref.getScaleBarColor();
        	if (pref.isFieldSelected(ViewerPreferences.ZOOM_FACTOR))
        		zoomFactor = ZoomAction.getZoomFactor(pref.getZoomIndex());
        }
    }
    
    /**
     * Called by the <code>Browser</code> after creation to allow this
     * object to store a back reference to the embedding component.
     * 
     * @param component The embedding component.
     */
    void initialize(Browser component) { this.component = component; }
    
    /**
     * Sets the rendered image.
     * 
     * @param image The image to set.
     */
    void setRenderedImage(BufferedImage image)
    {
        renderedImage = image;
        //Create the annotate image.
        if (renderedImage != null) {
        	if (init) {
        		int imageWidth = image.getWidth();
        		if (imageWidth < ImViewer.MINIMUM_SIZE) {
        			ratio = 1;
        			gridRatio = 1;
        			unitBar = false;
        		}
        		if (imageWidth*ratio > ImViewer.MAXIMUM_SIZE)
        			ratio = (double) ImViewer.MAXIMUM_SIZE/imageWidth;
        		init = false;
        	}
        }
        displayedImage = null;
        combinedImage = null;
        gridImages.clear();
    }
    
    /** Sets the images composing the grid. */
    void setGridImages()
    {
    	if (gridImages.size() != 0) return;
    	if (originalGridImages != null) originalGridImages.clear();
    	createGridImages();
    }
    
    /**
     * Returns the image to paint on screen. This image is a transformed 
     * version of the rendered image. We apply several transformations to the
     * {@link #renderedImage} e.g. zooming.
     * 
     * @return See above.
     */
    BufferedImage getDisplayedImage() { return displayedImage; }
    
    /**
     * Returns the image to paint on screen. This image is a transformed 
     * version of the projected image. We apply several transformations to the
     * {@link #projectedImage} e.g. zooming.
     * 
     * @return See above.
     */
    BufferedImage getDisplayedProjectedImage()
    { 
    	return displayedProjectedImage;
    }
    
    /**
     * Returns the rendered image.
     * 
     * @return See above.
     */
    BufferedImage getRenderedImage() { return renderedImage; }
    
    /**
     * Sets the zoom factor.
     * 
     * @param factor See above.
     */
    void setZoomFactor(double factor) { zoomFactor = factor; }

    /**
     * Returns the zoom factor.
     * 
     * @return See above.
     */
    double getZoomFactor() { return zoomFactor; }
    
    /**
     * Returns the title of the {@link Browser}.
     * 
     * @return See above.
     */
    String getTitle() { return ImViewer.TITLE_VIEW_INDEX; }
    
    /**
     * Returns the icon of the {@link Browser}.
     * 
     * @return See above.
     */
    Icon getIcon()
    { 
    	IconManager icons = IconManager.getInstance();
    	return icons.getIcon(IconManager.VIEWER); 
    }
    
    /**
     * Creates the {@link #displayedImage}. The method should be invoked
     * after the {@link #setRenderedImage(BufferedImage)} method.
     */
    void createDisplayedImage()
    {
        if (renderedImage == null) return;
        if (zoomFactor != ZoomAction.DEFAULT_ZOOM_FACTOR) {
        	BufferedImage img = null;
        	try {
				img = Factory.magnifyImage(renderedImage, zoomFactor, 0);
			} catch (Exception e) {
				UserNotifier un = ImViewerAgent.getRegistry().getUserNotifier();
				un.notifyInfo("Magnification", 
						"An error occurs while magnifying the image.");
			}
			if (img != null) displayedImage = img;
        } else displayedImage = renderedImage;
    }
   
    /**
     * Creates the {@link #displayedImage}. The method should be invoked
     * after the {@link #setRenderedImage(BufferedImage)} method.
     */
    void createDisplayedProjectedImage()
    {
        if (projectedImage == null) return;
        if (zoomFactor != ZoomAction.DEFAULT_ZOOM_FACTOR) {
        	BufferedImage img = null;
        	try {
				img = Factory.magnifyImage(projectedImage, zoomFactor, 0);
			} catch (Exception e) {
				UserNotifier un = ImViewerAgent.getRegistry().getUserNotifier();
				un.notifyInfo("Magnification", 
						"An error occurs while magnifying the image.");
			}
			if (img != null) displayedProjectedImage = img;
        } else displayedProjectedImage = projectedImage;
    }
    
    /** 
     * Returns the number of z-sections. 
     * 
     * @return See above.
     */
    int getMaxZ() { return parent.getMaxZ(); }
    
    /** 
     * Returns the number of timepoints. 
     * 
     * @return See above.
     */
    int getMaxT() { return parent.getMaxT(); }
    
    /**
     * The size in microns of a pixel along the X-axis.
     * 
     * @return See above.
     */
    float getPixelsSizeX() { return parent.getPixelsSizeX(); }
    
    /**
     * Returns <code>true</code> if the unit bar is painted on top of 
     * the displayed image, <code>false</code> otherwise.
     * 
     * @return See above.
     */
    boolean isUnitBar() { return unitBar; }
    
    /**
     * Sets the value of the flag controlling if the unit bar is painted or not.
     * 
     * @param unitBar   Pass <code>true</code> to paint the unit bar, 
     *                  <code>false</code> otherwise.
     */
    void setUnitBar(boolean unitBar) { this.unitBar = unitBar; }
    
    /**
     * Sets the size of the unit bar.
     * 
     * @param size The size of the unit bar.
     */
    void setUnitBarSize(double size) { unitInMicrons = size; }
    
    /**
     * Returns the unit (in microns) used to determine the size of the unit bar.
     * 
     * @return See above.
     */
    double getUnitInMicrons() { return unitInMicrons; }
    
    /**
     * Returns the size of the unit bar.
     * 
     * @return See above.
     */
    double getUnitBarSize()
    { 
        double v = unitInMicrons;
        if (getPixelsSizeX() > 0) v = unitInMicrons/getPixelsSizeX();
        v *= zoomFactor;
        return v;
    }
    
    /**
     * Returns the size of the unit bar for an image composing the grid.
     * 
     * @return See above.
     */
    double getGridBarSize()
    {
    	double v = unitInMicrons;
        if (getPixelsSizeX() > 0) v = unitInMicrons/getPixelsSizeX();
        v *= gridRatio;
        return v;
    }
    
    /**
     * Returns the unit bar value.
     * 
     * @return See above.
     */
    String getUnitBarValue()
    {
    	return UIUtilities.twoDecimalPlaces(unitInMicrons);
    }
    
    /**
     * Returns the color of the unit bar.
     * 
     * @return See above.
     */
    Color getUnitBarColor() { return unitBarColor; }

    /**
     * Sets the color of the unit bar.
     * 
     * @param c  The color to set.
     */
    void setUnitBarColor(Color c) { unitBarColor = c; }

    /**
     * Returns the background color of the canvas.
     * 
     * @return See above.
     */
	Color getBackgroundColor() { return backgroundColor; }
	
    /**
     * Set the background color of the canvas.
     * 
     * @param c  The color to set.
     */
	void setBackgroundColor(Color c) { backgroundColor = c; }
	
	/**
	 * Returns the unloaded image data.
	 * 
	 * @return See above.
	 */
	ImageData getImageData() { return data; }

    /**
     * Returns the title of the <code>Grid View</code>.
     * 
     * @return See above.
     */
    String getGridViewTitle() { return ImViewer.TITLE_GRID_INDEX; }
    
    /**
     * Returns the icon of the <code>Annotator</code>.
     * 
     * @return See above.
     */
    Icon getGridViewIcon()
    { 
    	IconManager icons = IconManager.getInstance();
    	return icons.getIcon(IconManager.GRIDVIEW); 
    }
    
    /**
     * Returns the title of the <code>Grid View</code>.
     * 
     * @return See above.
     */
    String getProjectionViewTitle() { return ImViewer.TITLE_PROJECTION_INDEX; }
    
    /**
     * Returns the icon of the <code>Annotator</code>.
     * 
     * @return See above.
     */
    Icon getProjectionViewIcon()
    { 
    	IconManager icons = IconManager.getInstance();
    	return icons.getIcon(IconManager.PROJECTION); 
    }
    
    /**
     * Returns the icon of the <code>Annotator</code>.
     * 
     * @return See above.
     */
    Icon getAnnotatorIcon()
    { 
    	IconManager icons = IconManager.getInstance();
    	return icons.getIcon(IconManager.ANNOTATION); 
    }
    
    /**
     * Sets the selected XY-plane. A new plane is then rendered.
     * 
     * @param z The selected z-section.
     * @param t The selected timepoint.
     */
    void setSelectedXYPlane(int z, int t)
    {
    	parent.setSelectedXYPlane(z, t);
    }
    
    /**
     * Returns the default timepoint.
     * 
     * @return See above.
     */
	int getDefaultT() { return parent.getDefaultT(); }
	
    /**
     * Returns the default z-section.
     * 
     * @return See above.
     */
	int getDefaultZ() { return parent.getDefaultZ(); }
	
	/**
	 * Returns the number of pixels along the X-axis.
	 * 
	 * @return See above.
	 */
	int getMaxX() { return parent.getMaxX(); }
	
	/**
	 * Returns the number of pixels along the Y-axis.
	 * 
	 * @return See above.
	 */
	int getMaxY() { return parent.getMaxY(); }

    /**
     * Returns the size in microns of a pixel along the Y-axis.
     * 
     * @return See above.
     */
    float getPixelsSizeY() { return parent.getPixelsSizeY(); }

    /**
     * Returns the size in microns of a pixel along the Y-axis.
     * 
     * @return See above.
     */
    float getPixelsSizeZ() { return parent.getPixelsSizeZ(); }
   
    /** 
     * Returns the number of column for the grid.
     * 
     * @return See above.
     */
    int getGridColumn() { return 2; }
    
    /** 
     * Returns the number of row for the grid.
     * 
     * @return See above.
     */
    int getGridRow()
    {
    	int n = parent.getMaxC();
    	if (n <= 3) return 2;
    	int row = n/2;
    	if (n%2 != 0) row += 1; 
    	return row;
    }
    
    /**
     * Returns the size of the grid.
     * 
     * @return See above.
     */
    Dimension getGridSize()
    {
    	int w = (int) (getMaxX()*gridRatio);
    	int h = (int) (getMaxY()*gridRatio);
    	int n = parent.getMaxC();
    	int row = 0;
    	int col = 0;
    	if (n <= 3) {
    		row = 2;
    		col = 2;
    	} else {
    		col = 3;
    		row = n/2;
    		if (n%2 != 0) row += 1; 
    	}
    	/*
    	 * int n = parent.getMaxC()+1; //add one for combined image.
    	if (n <= 3) n = 4;
    	int index = 0;
    	if (n%2 != 0) index = 1;
    	int col = (int) Math.floor(Math.sqrt(n))+index; 
    	int row = n/col+index;
    	*/

    	return new Dimension(col*w+(col-1)*GAP, row*h+(row-1)*GAP);
    }
    
    /** 
     * Returns <code>true</code> if there is no images retrieved for the
     * grid view, <code>false</code> otherwise.
     * 
     * @return See above.
     */
    boolean hasNoGridImages() { return (gridImages.size() == 0); }
    
    /**
     * Returns a collection of images composing the grid.
     * 
     * @return See above.
     */
    List getSplitImages()
    { 
    	if (splitImages == null) splitImages = new ArrayList<SplitImage>();
    	else splitImages.clear();
    	BufferedImage combined;
    	String n;
    	combined = combinedImage;
    	List<ChannelData> list = parent.getSortedChannelData();
    	Iterator<ChannelData> i = list.iterator();
    	ChannelData channel;
    	int j = 0;
    	while (i.hasNext()) {
    		channel = i.next();
    		n = PREFIX+channel.getChannelLabeling();
    		splitImages.add(new SplitImage(gridImages.get(j), n));
    		j++;
		}
    	splitImages.add(new SplitImage(combined, COMBINED));
    	return splitImages;
    }
    
    /**
     * Returns the index of the selected pane. 
     * 
     * @return See above.
     */
    int getSelectedIndex() { return parent.getSelectedIndex(); }
	
	/**
	 * Returns <code>true</code> if the textual information is painted on 
	 * top of the grid image, <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	boolean isTextVisible() { return parent.isTextVisible(); }

	/**
	 * Returns the magnification factor used to render the annotate image.
	 * 
	 * @return See above.
	 */
	double getRatio() { return ratio; }
	
	/**
	 * Returns the magnification factor used to render a grid image.
	 * 
	 * @return See above.
	 */
	double getGridRatio() { return gridRatio; }

	/** 
	 * Sets the ratio of the grid image.
	 * 
	 * @param gridRatio The value to set. 
	 */
	void setGridRatio(double gridRatio)
	{ 
		//if (ratio == 1) return; //We don't want to be too small.
		double max = ZoomGridAction.MAX_ZOOM_FACTOR;
		if (gridRatio > max) return;
		this.gridRatio = gridRatio; 
		if (originalGridImages == null) {
			createGridImages(); 
			return;
		}
		int n = originalGridImages.size();
		gridImages.clear();
		int maxC = parent.getMaxC();
		//List<BufferedImage> images = new ArrayList<BufferedImage>(maxC);
		switch (n) {
			case 0:
				for (int i = 0; i < maxC; i++) 
					gridImages.add(null);
				break;
			case 1:
			case 2:
			case 3:
				//TODO: Review that code.
				if (isImageRGB(parent.getActiveChannels())) {
					createGridImages(); 
				} else {
					combinedImage = Factory.magnifyImage(gridRatio, 
														renderedImage);
					Iterator i = originalGridImages.iterator();
					while (i.hasNext()) {
    	        		gridImages.add(Factory.magnifyImage(gridRatio, 
    	        							(BufferedImage) i.next()));
    	    		}
				}
				break;
			default:
				combinedImage = Factory.magnifyImage(gridRatio, renderedImage);
				Iterator i = originalGridImages.iterator();
				while (i.hasNext()) {
		    		gridImages.add(Factory.magnifyImage(gridRatio, 
		    							(BufferedImage) i.next()));
				}
		}
	}
	
	/**
	 * Returns the combined image, displayed in the grid view.
	 * 
	 * @return See above.
	 */
	BufferedImage getCombinedImage() { return combinedImage; }
	
	/** 
	 * Returns the number of the channels.
	 * 
	 * @return See above.
	 */
	int getMaxC() { return parent.getMaxC(); }

	/**
	 * Zooms in and out the image depending on the passed parameter.
	 * 
	 * @param increase 	Pass <code>true</code> to zoom in, <code>false</code>
	 * 					to zoom out.
	 */
	void zoom(boolean increase)
	{
		int index = ZoomCmd.ZOOM_IN;
		if (!increase) index = ZoomCmd.ZOOM_OUT;
		ZoomCmd cmd = new ZoomCmd(parent, index);
		cmd.execute();
	}

	/**
	 * Returns the projected image if any.
	 * 
	 * @return See above.
	 */
	BufferedImage getProjectedImage() { return projectedImage; }

	/**
	 * Returns the projected image if any.
	 * 
	 * @param projectedImage The projected image.
	 */
	void setProjectedImage(BufferedImage projectedImage)
	{
		this.projectedImage = projectedImage;
	}

	/** Builds a projected image to preview. */
	void projectionPreview() { parent.renderXYPlane(); }
	
	/**
	 * Returns the parent model.
	 * 
	 * @return See above.
	 */
	ImViewer getParentModel() { return parent; }

	/** Clears the grid images when the color model changes. */
	void clearGridImages()
	{ 
		if (gridImages != null) gridImages.clear(); 
		//if (originalGridImages != null) originalGridImages.clear();
	}
	
}