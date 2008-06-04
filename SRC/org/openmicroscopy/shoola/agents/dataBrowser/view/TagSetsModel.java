/*
 * org.openmicroscopy.shoola.agents.dataBrowser.view.TagSetsModel 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
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
package org.openmicroscopy.shoola.agents.dataBrowser.view;


//Java imports
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.dataBrowser.DataBrowserAgent;
import org.openmicroscopy.shoola.agents.dataBrowser.DataBrowserLoader;
import org.openmicroscopy.shoola.agents.dataBrowser.DataBrowserTranslator;
import org.openmicroscopy.shoola.agents.dataBrowser.ThumbnailLoader;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.BrowserFactory;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.ImageNode;
import pojos.ImageData;
import pojos.TagAnnotationData;

/** 
 * 
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
class TagSetsModel 	
	extends DataBrowserModel
{

	/** The collection of objects this model is for. */
	private Set<TagAnnotationData> tagSets;

	/**
	 * Creates a new instance.
	 * 
	 * @param parent	The parent of the datasets.
	 * @param tagSets 	The collection to datasets the model is for.
	 */
	TagSetsModel(Object parent, Set<TagAnnotationData> tagSets)
	{
		super();
		if (tagSets  == null) 
			throw new IllegalArgumentException("No images.");
		this.tagSets = tagSets;
		this.parent = parent;
		long userID = DataBrowserAgent.getUserDetails().getId();
		Set visTrees = DataBrowserTranslator.transformHierarchy(tagSets, 
							userID, 0);
        browser = BrowserFactory.createBrowser(visTrees);
        layoutBrowser();
        Iterator<TagAnnotationData> i = tagSets.iterator();
        TagAnnotationData tag;
		while (i.hasNext()) {
			tag = i.next();
			numberOfImages += tag.getImages().size();
		}
	}
	
	/**
	 * Creates a concrete loader.
	 * @see DataBrowserModel#createDataLoader(boolean, Collection)
	 */
	protected DataBrowserLoader createDataLoader(boolean refresh, 
			Collection ids)
	{
		if (refresh) imagesLoaded = 0;
		if (imagesLoaded == numberOfImages) return null;
		//only load thumbnails not loaded.
		List<ImageNode> nodes = browser.getVisibleImageNodes();
		if (nodes == null || nodes.size() == 0) return null;
		Iterator<ImageNode> i = nodes.iterator();
		ImageNode node;
		List<ImageData> imgs = new ArrayList<ImageData>();
		if (ids != null) {
			ImageData img;
			while (i.hasNext()) {
				node = i.next();
				img = (ImageData) node.getHierarchyObject();
				if (ids.contains(img.getId())) {
					if (node.getThumbnail().getFullScaleThumb() == null) {
						imgs.add((ImageData) node.getHierarchyObject());
						imagesLoaded++;
					}
				}
			}
		} else {
			while (i.hasNext()) {
				node = i.next();
				if (node.getThumbnail().getFullScaleThumb() == null) {
					imgs.add((ImageData) node.getHierarchyObject());
					imagesLoaded++;
				}
			}
		}
		
		return new ThumbnailLoader(component, sorter.sort(imgs));
	}

	/**
	 * Creates a concrete loader.
	 * @see DataBrowserModel#reloadThumbnails(Collection)
	 */
	protected DataBrowserLoader reloadThumbnails(Collection ids)
	{
		return null;
	}
	
	/**
	 * Returns the type of this model.
	 * @see DataBrowserModel#getType()
	 */
	protected int getType() { return DataBrowserModel.TAGSETS; }
	
}
