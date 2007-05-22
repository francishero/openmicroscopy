/*
 * org.openmicroscopy.shoola.util.roi.io.OutputStrategy 
 *
  *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2007 University of Dundee. All rights reserved.
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
package org.openmicroscopy.shoola.util.roi.io;

//Java imports
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

//Third-party libraries
import static org.jhotdraw.draw.AttributeKeys.FILL_COLOR;
import static org.jhotdraw.draw.AttributeKeys.FONT_BOLD;
import static org.jhotdraw.draw.AttributeKeys.FONT_FACE;
import static org.jhotdraw.draw.AttributeKeys.FONT_ITALIC;
import static org.jhotdraw.draw.AttributeKeys.FONT_SIZE;
import static org.jhotdraw.draw.AttributeKeys.TEXT_COLOR;
import static org.jhotdraw.draw.AttributeKeys.STROKE_CAP;
import static org.jhotdraw.draw.AttributeKeys.STROKE_COLOR;
import static org.jhotdraw.draw.AttributeKeys.STROKE_DASHES;
import static org.jhotdraw.draw.AttributeKeys.STROKE_DASH_PHASE;
import static org.jhotdraw.draw.AttributeKeys.STROKE_JOIN;
import static org.jhotdraw.draw.AttributeKeys.STROKE_MITER_LIMIT;
import static org.jhotdraw.draw.AttributeKeys.STROKE_WIDTH;
import static org.jhotdraw.draw.AttributeKeys.WINDING_RULE;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.FILL_GRADIENT;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.FILL_OPACITY;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_GRADIENT;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_OPACITY;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.TRANSFORM;

import org.jhotdraw.draw.AttributeKey;
import org.jhotdraw.draw.BezierFigure;
import org.jhotdraw.draw.TextFigure;
import org.jhotdraw.draw.TextHolderFigure;
import org.jhotdraw.draw.AttributeKeys.WindingRule;
import org.jhotdraw.geom.BezierPath;
import org.jhotdraw.samples.svg.LinearGradient;
import org.jhotdraw.samples.svg.RadialGradient;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.XMLElement;
import net.n3.nanoxml.XMLWriter;

//Application-internal dependencies
import org.openmicroscopy.shoola.util.roi.figures.BezierAnnotationFigure;
import org.openmicroscopy.shoola.util.roi.figures.EllipseAnnotationFigure;
import org.openmicroscopy.shoola.util.roi.figures.LineAnnotationFigure;
import org.openmicroscopy.shoola.util.roi.figures.LineConnectionAnnotationFigure;
import org.openmicroscopy.shoola.util.roi.figures.MeasureTextFigure;
import org.openmicroscopy.shoola.util.roi.figures.ROIFigure;
import org.openmicroscopy.shoola.util.roi.figures.RectAnnotationFigure;
import org.openmicroscopy.shoola.util.roi.ROIComponent;
import org.openmicroscopy.shoola.util.roi.model.ROI;
import org.openmicroscopy.shoola.util.roi.model.ROIShape;
import org.openmicroscopy.shoola.util.roi.model.annotation.AnnotationKey;
import org.openmicroscopy.shoola.util.roi.model.util.Coord3D;

/** 
 * 
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class OutputStrategy 
{
	public final static String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
	public final static String ROI_NAMESPACE = "http://www.openmicroscopy.org.uk";
	public final static String VERSION_TAG = "version";
	public final static String SVG_VERSION = "1.2";
	public final static String SVG_XLINK_VALUE = "http://www.w3.org/1999/xlink";
	public final static String XLINK_ATTRIBUTE = "xmlns:xlink";
	
	public final static String ROI_VERSION = "1.0";
	public final static String ROISET_TAG = "roiset";
	public final static String ROI_TAG = "roi";
	public final static String ROI_ID_ATTRIBUTE = "id";
	public final static String ROISHAPE_TAG = "roishape";
	public final static String ANNOTATION_TAG = "annotation";
	public final static String DEFS_TAG = "defs";
	public final static String SVG_TAG = "svg";
	public final static String VALUE_TAG = "value";
	public final static String RECT_TAG = "rect";
	public final static String ELLIPSE_TAG = "ellipse";
	public final static String LINE_TAG = "line";
	public final static String TEXT_TAG = "text";
	public final static String POLYLINE_TAG = "polyline";
	public final static String POLYGON_TAG = "polygon";
	
	public final static String DATATYPE_ATTRIBUTE = "type";
	public final static String SIZE_ATTRIBUTE = "size"; 
	public final static String VALUE_ATTRIBUTE = "value";
	public final static String POINTS_ATTRIBUTE = "points";
	
	public final static String CONNECTION_TO_ATTRIBUTE = "to";
	public final static String CONNECTION_FROM_ATTRIBUTE = "from";
	public final static String X_ATTRIBUTE = "x";
	public final static String X1_ATTRIBUTE = "x1";
	public final static String X2_ATTRIBUTE = "x2";
	public final static String Y_ATTRIBUTE = "y";
	public final static String Y1_ATTRIBUTE = "y1";
	public final static String Y2_ATTRIBUTE = "y2";
	public final static String CX_ATTRIBUTE = "cx";
	public final static String CY_ATTRIBUTE = "cy";
	public final static String RX_ATTRIBUTE = "rx";
	public final static String RY_ATTRIBUTE = "ry";
	public final static String Z_ATTRIBUTE = "z";
	public final static String C_ATTRIBUTE = "c";
	public final static String T_ATTRIBUTE = "t";
	public final static String WIDTH_ATTRIBUTE = "width";
	public final static String HEIGHT_ATTRIBUTE = "height";
	public final static String RED_ATTRIBUTE = "r";
	public final static String BLUE_ATTRIBUTE = "b";
	public final static String GREEN_ATTRIBUTE = "g";
	public final static String ALPHA_ATTRIBUTE = "a";
	
	
	public final static String ATTRIBUTE_DATATYPE_STRING = "String";
	public final static String ATTRIBUTE_DATATYPE_DOUBLE = "Double";
	public final static String ATTRIBUTE_DATATYPE_LONG = "Long";
	public final static String ATTRIBUTE_DATATYPE_INTEGER = "Integer";
	public final static String ATTRIBUTE_DATATYPE_FLOAT = "Float";
	public final static String ATTRIBUTE_DATATYPE_POINT2D = "Point2D";
	public final static String ATTRIBUTE_DATATYPE_ELLIPSE2D = "Ellipse2D";
	public final static String ATTRIBUTE_DATATYPE_RECTANGLE2D = "Rectangle2D";
	public final static String ATTRIBUTE_DATATYPE_COLOUR = "Color";
	public final static String ATTRIBUTE_DATATYPE_COORD3D = "Coord3D";
	public final static String ATTRIBUTE_DATATYPE_ARRAYLIST = "ArrayList";
	
	
	
	private final static HashMap<Integer, String> strokeLinejoinMap;
	static 
    {
        strokeLinejoinMap = new HashMap<Integer, String>();
        strokeLinejoinMap.put(BasicStroke.JOIN_MITER, "miter");
        strokeLinejoinMap.put(BasicStroke.JOIN_ROUND, "round");
        strokeLinejoinMap.put(BasicStroke.JOIN_BEVEL, "bevel");
    }
    
    private final static HashMap<Integer, String> strokeLinecapMap;
    static 
    {
        strokeLinecapMap = new HashMap<Integer, String>();
        strokeLinecapMap.put(BasicStroke.CAP_BUTT, "butt");
        strokeLinecapMap.put(BasicStroke.CAP_ROUND, "round");
        strokeLinecapMap.put(BasicStroke.CAP_SQUARE, "square");
    }
	
    private IXMLElement document;
    private IXMLElement defs; 
	
	 /**
    * This is a counter used to create the next unique identification.
    */
   private int nextId;
   
   /**
    * In this hash map we store all elements to which we have assigned
    * an id.
    */
   private HashMap<IXMLElement,String> identifiedElements;
   
   OutputStrategy()
   {
	   nextId = 0;
   }

   /**
   * Gets a unique ID for the specified element.
   */
   private String getId(IXMLElement element) 
   {
	   if (identifiedElements.containsKey(element)) 
	   {
           return identifiedElements.get(element);
       } 
	   else 
       {
           String id = Integer.toString(nextId++, Character.MAX_RADIX);
           identifiedElements.put(element, id);
           return id;
       }
   }
  	
   public void write(OutputStream out, ROIComponent roiComponent) throws IOException 
   {
		document = new XMLElement(ROISET_TAG, ROI_NAMESPACE);
		document.setAttribute(VERSION_TAG, ROI_VERSION);
		defs = new XMLElement(DEFS_TAG);
		document.addChild(defs);
		ROIComponent collection = roiComponent;
	        
	    TreeMap<Long, ROI> roiMap = collection.getROIMap();
	    Iterator iterator = roiMap.values().iterator();
	        
	    while(iterator.hasNext())
	    {
	    	write(document, (ROI)iterator.next());
	    }
	        
	    new XMLWriter(out).write(document);
	}

	private void write(IXMLElement document, ROI roi) throws IOException
	{
		XMLElement roiElement = new XMLElement(ROI_TAG);
		document.addChild(roiElement);

		writeROIAnnotations(roiElement, roi);
		
		TreeMap<Coord3D, ROIShape> roiShapes = roi.getShapes();
	    Iterator iterator = roiShapes.values().iterator();
	    
	    while(iterator.hasNext())
	    	writeROIShape(roiElement, (ROIShape)iterator.next());
	}

	private void writeROIAnnotations(IXMLElement roiElement, ROI roi)
	{
		roiElement.setAttribute(ROI_ID_ATTRIBUTE, roi.getID()+"");
		Map<AnnotationKey, Object> annotationMap = roi.getAnnotation();
		Iterator iterator = annotationMap.keySet().iterator();
		while(iterator.hasNext())
		{
			AnnotationKey key = (AnnotationKey)iterator.next();
			XMLElement annotation = new XMLElement(key.getKey());
			addAttributes(annotation, annotationMap.get(key));
		}
	}

	private void writeROIShapeAnnotations(IXMLElement shapeElement, ROIShape shape)
	{
		Map<AnnotationKey, Object> annotationMap = shape.getAnnotation();
		IXMLElement annotationLeaf = new XMLElement(ANNOTATION_TAG);
		Iterator iterator = annotationMap.keySet().iterator();
		while(iterator.hasNext())
		{
			AnnotationKey key = (AnnotationKey)iterator.next();
			XMLElement annotation = new XMLElement(key.getKey());
			addAttributes(annotation, annotationMap.get(key));
			annotationLeaf.addChild(annotation);
		}
		shapeElement.addChild(annotationLeaf);
	}
	
	private void addAttributes(XMLElement annotation, Object value)
	{
		String str;
		if( value instanceof Double || 
			value instanceof Float ||
			value instanceof Integer ||
			value instanceof Long)
		{
			if( value instanceof Double)
				annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_DOUBLE);
			if( value instanceof Float)
				annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_FLOAT);
			if( value instanceof Integer)
				annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_INTEGER);
			if( value instanceof Long)
				annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_LONG);
			annotation.setAttribute(VALUE_ATTRIBUTE, value+"");
		}
		else if(value instanceof Color)
		{
			Color colour = (Color)value;
			annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_COLOUR);
			annotation.setAttribute(RED_ATTRIBUTE, colour.getRed()+"");
			annotation.setAttribute(GREEN_ATTRIBUTE, colour.getGreen()+"");
			annotation.setAttribute(BLUE_ATTRIBUTE, colour.getBlue()+"");
			annotation.setAttribute(ALPHA_ATTRIBUTE, colour.getAlpha()+"");
		}
		else if(value instanceof Rectangle2D)
		{
			Rectangle2D object = (Rectangle2D)value;
			annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_RECTANGLE2D);
			annotation.setAttribute(X_ATTRIBUTE, object.getX()+"");
			annotation.setAttribute(Y_ATTRIBUTE, object.getY()+"");
			annotation.setAttribute(WIDTH_ATTRIBUTE, object.getWidth()+"");
			annotation.setAttribute(HEIGHT_ATTRIBUTE, object.getHeight()+"");
		}	
		else if(value instanceof Ellipse2D)
		{
			Ellipse2D object = (Ellipse2D)value;
			annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_ELLIPSE2D);
			annotation.setAttribute(X_ATTRIBUTE, object.getX()+"");
			annotation.setAttribute(Y_ATTRIBUTE, object.getY()+"");
			annotation.setAttribute(WIDTH_ATTRIBUTE, object.getWidth()+"");
			annotation.setAttribute(HEIGHT_ATTRIBUTE, object.getHeight()+"");
		}
		else if(value instanceof String)
		{
			annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_STRING);
			annotation.setAttribute(VALUE_ATTRIBUTE, (String)value);
		}
		else if(value instanceof Point2D)
		{
			Point2D point = (Point2D)value;
			annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_POINT2D);
			annotation.setAttribute(X_ATTRIBUTE, point.getX()+"");
			annotation.setAttribute(Y_ATTRIBUTE, point.getY()+"");
		}
		else if(value instanceof Coord3D)
		{
			Coord3D coord = (Coord3D)value;
			annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_COORD3D);
			annotation.setAttribute(T_ATTRIBUTE, coord.getTimePoint()+"");
			annotation.setAttribute(Z_ATTRIBUTE, coord.getZSection()+"");
		}
		else if(value instanceof ArrayList)
		{
			ArrayList list = (ArrayList)value;
			annotation.setAttribute(DATATYPE_ATTRIBUTE, ATTRIBUTE_DATATYPE_ARRAYLIST);
			annotation.setAttribute(SIZE_ATTRIBUTE, list.size()+"");
			for( int i = 0 ; i < list.size(); i++)
			{
				XMLElement valueElement = new XMLElement(VALUE_TAG);
				Object object = list.get(i);
				addAttributes(valueElement, object);
				annotation.addChild(valueElement);
			}
		}
	}

	private String newLine()
	{
		return System.getProperty("line.separator");
	}
	
	private void writeROIShape(XMLElement roiElement, ROIShape shape) throws IOException
	{
		XMLElement shapeElement = new XMLElement(ROISHAPE_TAG);
		roiElement.addChild(shapeElement);
		shapeElement.setAttribute(T_ATTRIBUTE,shape.getCoord3D().getTimePoint()+"");
		shapeElement.setAttribute(Z_ATTRIBUTE,shape.getCoord3D().getZSection()+"");
		writeROIShapeAnnotations(shapeElement, shape);
		ROIFigure figure = shape.getFigure();
		figure.calculateMeasurements();
		writeFigure(shapeElement, figure);
	}

	private void writeFigure(XMLElement shapeElement, ROIFigure figure) throws IOException
	{
		/*** TO DO ****/
		//double fillOpacity = FILL_COLOR.get(figure).getAlpha()/255.0;
		//FILL_OPACITY.set(figure, fillOpacity);
		//double strokeOpacity = STROKE_COLOR.get(figure).getAlpha()/255.0;
		//STROKE_OPACITY.set(figure, strokeOpacity);
		/*** TO DO ****/
		if(figure instanceof RectAnnotationFigure)
		{
			writeSVGHeader(shapeElement);
			writeRectAnnotationFigure(shapeElement, (RectAnnotationFigure)figure);
	        writeTextFigure(shapeElement, (RectAnnotationFigure)figure);
		}
		else if (figure instanceof EllipseAnnotationFigure)
		{
			writeSVGHeader(shapeElement);
			writeEllipseAnnotationFigure(shapeElement, (EllipseAnnotationFigure)figure);		
			writeTextFigure(shapeElement, (EllipseAnnotationFigure)figure);
		}
		else if(figure instanceof LineConnectionAnnotationFigure)
		{
			writeSVGHeader(shapeElement);
			writeLineConnectionFigure(shapeElement, (LineConnectionAnnotationFigure)figure);
			writeTextFigure(shapeElement, (LineConnectionAnnotationFigure)figure);
		}
		else if (figure instanceof BezierAnnotationFigure)
		{
			writeSVGHeader(shapeElement);
			writeBezierAnnotationFigure(shapeElement, (BezierAnnotationFigure)figure);		
			writeTextFigure(shapeElement, (BezierAnnotationFigure)figure);
		}
		else if (figure instanceof LineAnnotationFigure)
		{
			writeSVGHeader(shapeElement);
			writeLineAnnotationFigure(shapeElement, (LineAnnotationFigure)figure);
			writeTextFigure(shapeElement, (LineAnnotationFigure)figure);
		}
		else if(figure instanceof MeasureTextFigure)
		{
			writeSVGHeader(shapeElement);
			writeTextFigure(shapeElement, (MeasureTextFigure)figure);
		}
	}

	private void writeSVGHeader(XMLElement shapeElement)
	{
		XMLElement svgElement = new XMLElement(SVG_TAG, SVG_NAMESPACE);
		svgElement.setAttribute(XLINK_ATTRIBUTE,SVG_XLINK_VALUE);
		svgElement.setAttribute(VERSION_TAG,SVG_VERSION);
		shapeElement.addChild(svgElement);
	}
	
	private void writeTextFigure(XMLElement shapeElement, MeasureTextFigure fig) throws IOException
	{
		writeTextFigure(shapeElement, (TextFigure)fig);
	}
	
	private void writeTextFigure(XMLElement shapeElement, TextHolderFigure fig) throws IOException
	{
		XMLElement textElement = new XMLElement(TEXT_TAG);
		IXMLElement svgElement = shapeElement.getFirstChildNamed(SVG_TAG);
		svgElement.addChild(textElement);
		
		textElement.setContent(fig.getText());
		textElement.setAttribute(X_ATTRIBUTE, fig.getStartPoint().getX()+"");
		textElement.setAttribute(Y_ATTRIBUTE, fig.getStartPoint().getY()+"");
		writeShapeAttributes(textElement, fig.getAttributes());
      	writeTransformAttribute(textElement, fig.getAttributes());
        writeFontAttributes(textElement, fig.getAttributes());
	}
	
	private void writeTextFigure(XMLElement shapeElement, TextFigure fig) throws IOException
	{
		XMLElement textElement = new XMLElement(TEXT_TAG);
		IXMLElement svgElement = shapeElement.getFirstChildNamed(SVG_TAG);
		svgElement.addChild(textElement);
		
		textElement.setContent(fig.getText());
		textElement.setAttribute(X_ATTRIBUTE, fig.getStartPoint().getX()+"");
		textElement.setAttribute(Y_ATTRIBUTE, fig.getStartPoint().getY()+"");
		writeShapeAttributes(textElement, fig.getAttributes());
      	writeTransformAttribute(textElement, fig.getAttributes());
        writeFontAttributes(textElement, fig.getAttributes());
	}
	
	private void writeLineConnectionFigure(XMLElement shapeElement, LineConnectionAnnotationFigure fig) throws IOException
	{
		IXMLElement svgElement = shapeElement.getFirstChildNamed(SVG_TAG);
		XMLElement lineConnectionElement = new XMLElement(LINE_TAG);
	  	svgElement.addChild(lineConnectionElement);
      
		ROIFigure startConnection = (ROIFigure)fig.getStartConnector().getOwner();
		ROIFigure endConnection = (ROIFigure)fig.getEndConnector().getOwner();
		lineConnectionElement.setAttribute(CONNECTION_FROM_ATTRIBUTE, startConnection.getROI().getID()+"");
		lineConnectionElement.setAttribute(CONNECTION_TO_ATTRIBUTE, endConnection.getROI().getID()+"");
		if(fig.getNodeCount()==2)
		{
			lineConnectionElement.setAttribute(X1_ATTRIBUTE, fig.getNode(0).x[0]+"");
			lineConnectionElement.setAttribute(Y1_ATTRIBUTE, fig.getNode(0).y[0]+"");
			lineConnectionElement.setAttribute(X2_ATTRIBUTE, fig.getNode(1).x[0]+"");
			lineConnectionElement.setAttribute(Y2_ATTRIBUTE, fig.getNode(1).y[0]+"");
		}
		else
		{
			LinkedList<Point2D.Double> points = new LinkedList<Point2D.Double>();
			BezierPath bezier = fig.getBezierPath();
		    for (BezierPath.Node node: bezier) 
		    {
		    	points.add(new Point2D.Double(node.x[0], node.y[0]));
		    }
		    String pointsValues = toPoints(points.toArray(new Point2D.Double[points.size()]));
		    lineConnectionElement.setAttribute(POINTS_ATTRIBUTE, pointsValues);
		}
      	writeShapeAttributes(lineConnectionElement, fig.getAttributes());
      	writeTransformAttribute(lineConnectionElement, fig.getAttributes());
  	}

	
	private void writeBezierAnnotationFigure(XMLElement shapeElement, BezierAnnotationFigure fig) throws IOException
	{
		IXMLElement svgElement = shapeElement.getFirstChildNamed(SVG_TAG);
		if(fig.isClosed())
			writePolygonFigure(svgElement, fig);
		else
			writePolylineFigure(svgElement, fig);
	}

	private void writePolygonFigure(IXMLElement svgElement, BezierAnnotationFigure fig) throws IOException
	{
		XMLElement bezierElement = new XMLElement(POLYGON_TAG);
		svgElement.addChild(bezierElement);
		
		LinkedList<Point2D.Double> points = new LinkedList<Point2D.Double>();
		BezierPath bezier = fig.getBezierPath();
	    for (BezierPath.Node node: bezier) 
	    {
	    	points.add(new Point2D.Double(node.x[0], node.y[0]));
	    }
	    String pointsValues = toPoints(points.toArray(new Point2D.Double[points.size()]));
	    bezierElement.setAttribute(POINTS_ATTRIBUTE, pointsValues);
	    writeShapeAttributes(bezierElement, fig.getAttributes());
	    writeTransformAttribute(bezierElement, fig.getAttributes());
	}
	
	private void writePolylineFigure(IXMLElement svgElement, BezierAnnotationFigure fig) throws IOException
	{
		XMLElement bezierElement = new XMLElement(POLYLINE_TAG);
		svgElement.addChild(bezierElement);
		
		LinkedList<Point2D.Double> points = new LinkedList<Point2D.Double>();
		BezierPath bezier = fig.getBezierPath();
	    for (BezierPath.Node node: bezier) 
	    {
	    	points.add(new Point2D.Double(node.x[0], node.y[0]));
	    }
	    String pointsValues = toPoints(points.toArray(new Point2D.Double[points.size()]));
	    bezierElement.setAttribute(POINTS_ATTRIBUTE, pointsValues);
	    writeShapeAttributes(bezierElement, fig.getAttributes());
	    writeTransformAttribute(bezierElement, fig.getAttributes());
	}
	    
	private void writeLineAnnotationFigure(XMLElement shapeElement, LineAnnotationFigure fig) throws IOException
	{
		IXMLElement svgElement = shapeElement.getFirstChildNamed(SVG_TAG);
	  	XMLElement lineElement = new XMLElement(LINE_TAG);
	  	svgElement.addChild(lineElement);
      
		if(fig.getNodeCount()==2)
		{
			lineElement.setAttribute(X1_ATTRIBUTE, fig.getNode(0).x[0]+"");
			lineElement.setAttribute(Y1_ATTRIBUTE, fig.getNode(0).y[0]+"");
			lineElement.setAttribute(X2_ATTRIBUTE, fig.getNode(1).x[0]+"");
			lineElement.setAttribute(Y2_ATTRIBUTE, fig.getNode(1).y[0]+"");
		}
		else
		{
			LinkedList<Point2D.Double> points = new LinkedList<Point2D.Double>();
			BezierPath bezier = fig.getBezierPath();
		    for (BezierPath.Node node: bezier) 
		    {
		    	points.add(new Point2D.Double(node.x[0], node.y[0]));
		    }
		    String pointsValues = toPoints(points.toArray(new Point2D.Double[points.size()]));
		    lineElement.setAttribute(POINTS_ATTRIBUTE, pointsValues);
		}
      	writeShapeAttributes(lineElement, fig.getAttributes());
      	writeTransformAttribute(lineElement, fig.getAttributes());
  	}
    
   
	private void writeEllipseAnnotationFigure(XMLElement shapeElement, EllipseAnnotationFigure fig) throws IOException
	{
		IXMLElement svgElement = shapeElement.getFirstChildNamed(SVG_TAG);
		XMLElement ellipseElement = new XMLElement(ELLIPSE_TAG);
		svgElement.addChild(ellipseElement);
		
		double cx = fig.getX() + fig.getWidth() / 2d;		
		double cy = fig.getY() + fig.getHeight() / 2d;
		double rx = fig.getWidth() / 2d;
		double ry = fig.getHeight() / 2d;
		ellipseElement.setAttribute(CX_ATTRIBUTE, cx+"");
		ellipseElement.setAttribute(CY_ATTRIBUTE, cy+"");
		ellipseElement.setAttribute(RX_ATTRIBUTE, rx+"");
		ellipseElement.setAttribute(RY_ATTRIBUTE, ry+"");
        writeShapeAttributes(ellipseElement, fig.getAttributes());
        writeTransformAttribute(ellipseElement, fig.getAttributes());
	}

	
	private void writeRectAnnotationFigure(XMLElement shapeElement, RectAnnotationFigure fig) throws IOException
	{
		IXMLElement svgElement = shapeElement.getFirstChildNamed(SVG_TAG);
		XMLElement rectElement = new XMLElement(RECT_TAG);
		svgElement.addChild(rectElement);
		
		rectElement.setAttribute(X_ATTRIBUTE, fig.getX()+"");
		rectElement.setAttribute(Y_ATTRIBUTE, fig.getY()+"");
		rectElement.setAttribute(WIDTH_ATTRIBUTE, fig.getWidth()+"");
		rectElement.setAttribute(HEIGHT_ATTRIBUTE, fig.getHeight()+"");
        writeShapeAttributes(rectElement, fig.getAttributes());
        writeTransformAttribute(rectElement, fig.getAttributes());
	}

	private void writeShapeAttributes(IXMLElement shapeElement, ROIFigure fig)
	{
		Map<AttributeKey, Object> attributeMap = fig.getAttributes();
		Iterator<AttributeKey> iterator = attributeMap.keySet().iterator();
		while(iterator.hasNext())
		{
			
		}
	}	
	
	protected void writeShapeAttributes(IXMLElement elem, Map<AttributeKey,Object> f)
    throws IOException {
        Color color;
        String value;
        int intValue;
        
        //'color'
        // Value:  	<color> | inherit
        // Initial:  	 depends on user agent
        // Applies to:  	None. Indirectly affects other properties via currentColor
        // Inherited:  	 yes
        // Percentages:  	 N/A
        // Media:  	 visual
        // Animatable:  	 yes
        // Computed value:  	 Specified <color> value, except inherit
        //
        // Nothing to do: Attribute 'color' is not needed.
        
        //'color-rendering'
        // Value:  	 auto | optimizeSpeed | optimizeQuality | inherit
        // Initial:  	 auto
        // Applies to:  	 container elements , graphics elements and 'animateColor'
        // Inherited:  	 yes
        // Percentages:  	 N/A
        // Media:  	 visual
        // Animatable:  	 yes
        // Computed value:  	 Specified value, except inherit
        //
        // Nothing to do: Attribute 'color-rendering' is not needed.
        
        // 'fill'
        // Value:  	<paint> | inherit (See Specifying paint)
        // Initial:  	 black
        // Applies to:  	 shapes and text content elements
        // Inherited:  	 yes
        // Percentages:  	 N/A
        // Media:  	 visual
        // Animatable:  	 yes
        // Computed value:  	 "none", system paint, specified <color> value or absolute IRI
        Object gradient = FILL_GRADIENT.get(f);
        if (gradient != null) {
            IXMLElement gradientElem;
            if (gradient instanceof LinearGradient) {
                LinearGradient lg = (LinearGradient) gradient;
                gradientElem = createLinearGradient(document,
                        lg.getX1(), lg.getY1(),
                        lg.getX2(), lg.getY2(),
                        lg.getStopOffsets(),
                        lg.getStopColors(),
                        lg.isRelativeToFigureBounds()
                        );
            } else /*if (gradient instanceof RadialGradient)*/ {
                RadialGradient rg = (RadialGradient) gradient;
                gradientElem = createRadialGradient(document,
                        rg.getCX(), rg.getCY(),
                        rg.getR(),
                        rg.getStopOffsets(),
                        rg.getStopColors(),
                        rg.isRelativeToFigureBounds()
                        );
            }
            String id = getId(gradientElem);
            gradientElem.setAttribute("id","xml",id);
            defs.addChild(gradientElem);
            writeAttribute(elem, "fill", "url(#"+id+")", "#000");
        } else {
            writeAttribute(elem, "fill", toColor(FILL_COLOR.get(f)), "#000");
        }
        
        
        //'fill-opacity'
        //Value:  	 <opacity-value> | inherit
        //Initial:  	 1
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "fill-opacity", FILL_OPACITY.get(f), 1d);
        
        // 'fill-rule'
        // Value:	 nonzero | evenodd | inherit
        // Initial: 	 nonzero
        // Applies to:  	 shapes and text content elements
        // Inherited:  	 yes
        // Percentages:  	 N/A
        // Media:  	 visual
        // Animatable:  	 yes
        // Computed value:  	 Specified value, except inherit
        if (WINDING_RULE.get(f) != WindingRule.NON_ZERO) {
            writeAttribute(elem, "fill-rule", "evenodd", "nonzero");
        }
        
        //'stroke'
        //Value:  	<paint> | inherit (See Specifying paint)
        //Initial:  	 none
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 "none", system paint, specified <color> value
        // or absolute IRI
        gradient = STROKE_GRADIENT.get(f);
        if (gradient != null) {
            IXMLElement gradientElem;
            if (gradient instanceof LinearGradient) {
                LinearGradient lg = (LinearGradient) gradient;
                gradientElem = createLinearGradient(document,
                        lg.getX1(), lg.getY1(),
                        lg.getX2(), lg.getY2(),
                        lg.getStopOffsets(),
                        lg.getStopColors(),
                        lg.isRelativeToFigureBounds()
                        );
            } else /*if (gradient instanceof RadialGradient)*/ {
                RadialGradient rg = (RadialGradient) gradient;
                gradientElem = createRadialGradient(document,
                        rg.getCX(), rg.getCY(),
                        rg.getR(),
                        rg.getStopOffsets(),
                        rg.getStopColors(),
                        rg.isRelativeToFigureBounds()
                        );
            }
            String id = getId(gradientElem);
            gradientElem.setAttribute("id","xml",id);
            defs.addChild(gradientElem);
            writeAttribute(elem, "stroke", "url(#"+id+")", "none");
        } else {
            writeAttribute(elem, "stroke", toColor(STROKE_COLOR.get(f)), "none");
        }
        
        //'stroke-dasharray'
        //Value:  	 none | <dasharray> | inherit
        //Initial:  	 none
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes (non-additive)
        //Computed value:  	 Specified value, except inherit
        double[] dashes = STROKE_DASHES.get(f);
        if (dashes != null) {
            StringBuilder buf = new StringBuilder();
            for (int i=0; i < dashes.length; i++) {
                if (i != 0) {
                    buf.append(',');
                }
                buf.append(toNumber(dashes[i]));
            }
            writeAttribute(elem, "stroke-dasharray", buf.toString(), null);
        }
        
        //'stroke-dashoffset'
        //Value:  	<length> | inherit
        //Initial:  	 0
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-dashoffset", STROKE_DASH_PHASE.get(f), 0d);
        
        //'stroke-linecap'
        //Value:  	 butt | round | square | inherit
        //Initial:  	 butt
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-linecap", strokeLinecapMap.get(STROKE_CAP.get(f)), "butt");
        
        //'stroke-linejoin'
        //Value:  	 miter | round | bevel | inherit
        //Initial:  	 miter
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-linejoin", strokeLinejoinMap.get(STROKE_JOIN.get(f)), "miter");
        
        //'stroke-miterlimit'
        //Value:  	 <miterlimit> | inherit
        //Initial:  	 4
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-miterlimit", STROKE_MITER_LIMIT.get(f), 4d);
        
        //'stroke-opacity'
        //Value:  	 <opacity-value> | inherit
        //Initial:  	 1
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-opacity", STROKE_OPACITY.get(f), 1d);
        
        //'stroke-width'
        //Value:  	<length> | inherit
        //Initial:  	 1
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-width", STROKE_WIDTH.get(f), 1d);
    }
    /* Writes the transform attribute as specified in
     * http://www.w3.org/TR/SVGMobile12/coords.html#TransformAttribute
     *
     */
    protected void writeTransformAttribute(IXMLElement elem, Map<AttributeKey,Object> a)
    throws IOException {
        AffineTransform t = TRANSFORM.get(a);
        if (t != null) {
            writeAttribute(elem, "transform", toTransform(t), "none");
        }
    }
    /* Reads font attributes as listed in
     * http://www.w3.org/TR/SVGMobile12/feature.html#Font
     */
    private void writeFontAttributes(IXMLElement elem, Map<AttributeKey,Object> a)
    throws IOException {
        String value;
        double doubleValue;
        Object gradient = FILL_GRADIENT.get(a);
        if (gradient != null) {
            IXMLElement gradientElem;
            if (gradient instanceof LinearGradient) {
                LinearGradient lg = (LinearGradient) gradient;
                gradientElem = createLinearGradient(document,
                        lg.getX1(), lg.getY1(),
                        lg.getX2(), lg.getY2(),
                        lg.getStopOffsets(),
                        lg.getStopColors(),
                        lg.isRelativeToFigureBounds()
                        );
            } else /*if (gradient instanceof RadialGradient)*/ {
                RadialGradient rg = (RadialGradient) gradient;
                gradientElem = createRadialGradient(document,
                        rg.getCX(), rg.getCY(),
                        rg.getR(),
                        rg.getStopOffsets(),
                        rg.getStopColors(),
                        rg.isRelativeToFigureBounds()
                        );
            }
            String id = getId(gradientElem);
            gradientElem.setAttribute("id","xml",id);
            defs.addChild(gradientElem);
            writeAttribute(elem, "fill", "url(#"+id+")", "#000");
        } else {
            writeAttribute(elem, "fill", toColor(TEXT_COLOR.get(a)), "#000");
        }
       // 'font-family'
        // Value:  	[[ <family-name> |
        // <generic-family> ],]* [<family-name> |
        // <generic-family>] | inherit
        // Initial:  	depends on user agent
        // Applies to:  	text content elements
        // Inherited:  	yes
        // Percentages:  	N/A
        // Media:  	visual
        // Animatable:  	yes
        // Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "font-family", FONT_FACE.get(a).getFamily(), "Dialog");
        
        // 'font-size'
        // Value:  	<absolute-size> | <relative-size> |
        // <length> | inherit
        // Initial:  	medium
        // Applies to:  	text content elements
        // Inherited:  	yes, the computed value is inherited
        // Percentages:  	N/A
        // Media:  	visual
        // Animatable:  	yes
        // Computed value:  	 Absolute length
        writeAttribute(elem, "font-size", FONT_SIZE.get(a), 0d);
        
        // 'font-style'
        // Value:  	normal | italic | oblique | inherit
        // Initial:  	normal
        // Applies to:  	text content elements
        // Inherited:  	yes
        // Percentages:  	N/A
        // Media:  	visual
        // Animatable:  	yes
        // Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "font-style", (FONT_ITALIC.get(a)) ? "italic" : "normal", "normal");
        
        
        //'font-variant'
        //Value:  	normal | small-caps | inherit
        //Initial:  	normal
        //Applies to:  	text content elements
        //Inherited:  	yes
        //Percentages:  	N/A
        //Media:  	visual
        //Animatable:  	no
        //Computed value:  	 Specified value, except inherit
        // XXX - Implement me
        writeAttribute(elem, "font-variant", "normal", "normal");
        
        // 'font-weight'
        // Value:  	normal | bold | bolder | lighter | 100 | 200 | 300
        // | 400 | 500 | 600 | 700 | 800 | 900 | inherit
        // Initial:  	normal
        // Applies to:  	text content elements
        // Inherited:  	yes
        // Percentages:  	N/A
        // Media:  	visual
        // Animatable:  	yes
        // Computed value:  	 one of the legal numeric values, non-numeric
        // values shall be converted to numeric values according to the rules
        // defined below.
        writeAttribute(elem, "font-weight", (FONT_BOLD.get(a)) ? "bold" : "normal", "normal");
    }
    
    private static String toPath(BezierPath[] paths) {
        StringBuilder buf = new StringBuilder();
        
        for (int j=0; j < paths.length; j++) {
            BezierPath path = paths[j];
            
            if (path.size() == 0) {
                // nothing to do
            } else if (path.size() == 1) {
                BezierPath.Node current = path.get(0);
                buf.append("M ");
                buf.append(current.x[0]);
                buf.append(' ');
                buf.append(current.y[0]);
                buf.append(" L ");
                buf.append(current.x[0]);
                buf.append(' ');
                buf.append(current.y[0] + 1);
            } else {
                BezierPath.Node previous;
                BezierPath.Node current;
                
                previous = current = path.get(0);
                buf.append("M ");
                buf.append(current.x[0]);
                buf.append(' ');
                buf.append(current.y[0]);
                for (int i=1, n = path.size(); i < n; i++) {
                    previous = current;
                    current = path.get(i);
                    
                    if ((previous.mask & BezierPath.C2_MASK) == 0) {
                        if ((current.mask & BezierPath.C1_MASK) == 0) {
                            buf.append(" L ");
                            buf.append(current.x[0]);
                            buf.append(' ');
                            buf.append(current.y[0]);
                        } else {
                            buf.append(" Q ");
                            buf.append(current.x[1]);
                            buf.append(' ');
                            buf.append(current.y[1]);
                            buf.append(' ');
                            buf.append(current.x[0]);
                            buf.append(' ');
                            buf.append(current.y[0]);
                        }
                    } else {
                        if ((current.mask & BezierPath.C1_MASK) == 0) {
                            buf.append(" Q ");
                            buf.append(current.x[2]);
                            buf.append(' ');
                            buf.append(current.y[2]);
                            buf.append(' ');
                            buf.append(current.x[0]);
                            buf.append(' ');
                            buf.append(current.y[0]);
                        } else {
                            buf.append(" C ");
                            buf.append(previous.x[2]);
                            buf.append(' ');
                            buf.append(previous.y[2]);
                            buf.append(' ');
                            buf.append(current.x[1]);
                            buf.append(' ');
                            buf.append(current.y[1]);
                            buf.append(' ');
                            buf.append(current.x[0]);
                            buf.append(' ');
                            buf.append(current.y[0]);
                        }
                    }
                }
                if (path.isClosed()) {
                    if (path.size() > 1) {
                        previous = path.get(path.size() - 1);
                        current = path.get(0);
                        
                        if ((previous.mask & BezierPath.C2_MASK) == 0) {
                            if ((current.mask & BezierPath.C1_MASK) == 0) {
                                buf.append(" L ");
                                buf.append(current.x[0]);
                                buf.append(' ');
                                buf.append(current.y[0]);
                            } else {
                                buf.append(" Q ");
                                buf.append(current.x[1]);
                                buf.append(' ');
                                buf.append(current.y[1]);
                                buf.append(' ');
                                buf.append(current.x[0]);
                                buf.append(' ');
                                buf.append(current.y[0]);
                            }
                        } else {
                            if ((current.mask & BezierPath.C1_MASK) == 0) {
                                buf.append(" Q ");
                                buf.append(previous.x[2]);
                                buf.append(' ');
                                buf.append(previous.y[2]);
                                buf.append(' ');
                                buf.append(current.x[0]);
                                buf.append(' ');
                                buf.append(current.y[0]);
                            } else {
                                buf.append(" C ");
                                buf.append(previous.x[2]);
                                buf.append(' ');
                                buf.append(previous.y[2]);
                                buf.append(' ');
                                buf.append(current.x[1]);
                                buf.append(' ');
                                buf.append(current.y[1]);
                                buf.append(' ');
                                buf.append(current.x[0]);
                                buf.append(' ');
                                buf.append(current.y[0]);
                            }
                        }
                    }
                    buf.append(" Z");
                }
            }
        }
        return buf.toString();
    }
    
    protected void writeAttribute(IXMLElement elem, String name, String value, String defaultValue) {
        writeAttribute(elem, name, "", value, defaultValue);
    }
    protected void writeAttribute(IXMLElement elem, String name, String namespace, String value, String defaultValue) {
        if (! value.equals(defaultValue)) {
            elem.setAttribute(name, value);
        }
    }
    protected void writeAttribute(IXMLElement elem, String name, Color color, Color defaultColor) {
        writeAttribute(elem, name, SVG_NAMESPACE, toColor(color), toColor(defaultColor));
    }

    protected void writeAttribute(IXMLElement elem, String name, double value, double defaultValue) {
        writeAttribute(elem, name, SVG_NAMESPACE, value, defaultValue);
    }
    protected void writeAttribute(IXMLElement elem, String name, String namespace, double value, double defaultValue) {
        if (value != defaultValue) {
            elem.setAttribute(name, toNumber(value));
        }
    }

    
    /**
     * Returns a double array as a number attribute value.
     */
    private static String toNumber(double number) {
        String str = Double.toString(number);
        if (str.endsWith(".0")) {
            str = str.substring(0, str.length() -  2);
        }
        return str;
    }
    
    /**
     * Returns a Point2D.Double array as a Points attribute value.
     * as specified in http://www.w3.org/TR/SVGMobile12/shapes.html#PointsBNF
     */
    private static String toPoints(Point2D.Double[] points) throws IOException {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i < points.length; i++) {
            if (i != 0) {
                buf.append(", ");
            }
            buf.append(toNumber(points[i].x));
            buf.append(',');
            buf.append(toNumber(points[i].y));
        }
        return buf.toString();
    }
    
    /* Converts an AffineTransform into an SVG transform attribute value as specified in
     * http://www.w3.org/TR/SVGMobile12/coords.html#TransformAttribute
     */
    private static String toTransform(AffineTransform t) throws IOException {
        StringBuilder buf = new StringBuilder();
        switch (t.getType()) {
            case AffineTransform.TYPE_IDENTITY :
                buf.append("none");
                break;
            case AffineTransform.TYPE_TRANSLATION :
                // translate(<tx> [<ty>]), specifies a translation by tx and ty.
                // If <ty> is not provided, it is assumed to be zero.
                buf.append("translate(");
                buf.append(toNumber(t.getTranslateX()));
                if (t.getTranslateY() != 0d) {
                    buf.append(' ');
                    buf.append(toNumber(t.getTranslateY()));
                }
                buf.append(')');
                break;
                /*
            case AffineTransform.TYPE_GENERAL_ROTATION :
            case AffineTransform.TYPE_QUADRANT_ROTATION :
            case AffineTransform.TYPE_MASK_ROTATION :
                // rotate(<rotate-angle> [<cx> <cy>]), specifies a rotation by
                // <rotate-angle> degrees about a given point.
                // If optional parameters <cx> and <cy> are not supplied, the
                // rotate is about the origin of the current user coordinate
                // system. The operation corresponds to the matrix
                // [cos(a) sin(a) -sin(a) cos(a) 0 0].
                // If optional parameters <cx> and <cy> are supplied, the rotate
                // is about the point (<cx>, <cy>). The operation represents the
                // equivalent of the following specification:
                // translate(<cx>, <cy>) rotate(<rotate-angle>)
                // translate(-<cx>, -<cy>).
                buf.append("rotate(");
                buf.append(toNumber(t.getScaleX()));
                buf.append(')');
                break;*/
            case AffineTransform.TYPE_UNIFORM_SCALE :
                // scale(<sx> [<sy>]), specifies a scale operation by sx
                // and sy. If <sy> is not provided, it is assumed to be equal
                // to <sx>.
                buf.append("scale(");
                buf.append(toNumber(t.getScaleX()));
                buf.append(')');
                break;
            case AffineTransform.TYPE_GENERAL_SCALE :
            case AffineTransform.TYPE_MASK_SCALE :
                // scale(<sx> [<sy>]), specifies a scale operation by sx
                // and sy. If <sy> is not provided, it is assumed to be equal
                // to <sx>.
                buf.append("scale(");
                buf.append(toNumber(t.getScaleX()));
                buf.append(' ');
                buf.append(toNumber(t.getScaleY()));
                buf.append(')');
                break;
            default :
                // matrix(<a> <b> <c> <d> <e> <f>), specifies a transformation
                // in the form of a transformation matrix of six values.
                // matrix(a,b,c,d,e,f) is equivalent to applying the
                // transformation matrix [a b c d e f].
                buf.append("matrix(");
                double[] matrix = new double[6];
                t.getMatrix(matrix);
                for (int i=0; i < matrix.length; i++) {
                    if (i != 0) {
                        buf.append(' ');
                    }
                    buf.append(toNumber(matrix[i]));
                }
                buf.append(')');
                break;
        }
        
        return buf.toString();
    }
    
    private static String toColor(Color color) {
        if (color == null) {
            return "none";
        }
        
        
        String value;
        value = "000000"+Integer.toHexString(color.getRGB());
        value = "#"+value.substring(value.length() - 6);
        if (value.charAt(1) == value.charAt(2) &&
                value.charAt(3) == value.charAt(4) &&
                value.charAt(5) == value.charAt(6)) {
            value = "#"+value.charAt(1)+value.charAt(3)+value.charAt(5);
        }
        return value;
    }
    

    protected IXMLElement createLinearGradient(IXMLElement doc,
            double x1, double y1, double x2, double y2,
            double[] stopOffsets, Color[] stopColors,
            boolean isRelativeToFigureBounds) throws IOException {
        IXMLElement elem = doc.createElement("linearGradient");
        
        writeAttribute(elem, "x1", toNumber(x1), "0");
        writeAttribute(elem, "y1", toNumber(y1), "0");
        writeAttribute(elem, "x2", toNumber(x2), "1");
        writeAttribute(elem, "y2", toNumber(y2), "0");
        writeAttribute(elem, "gradientUnits", 
                (isRelativeToFigureBounds) ? "objectBoundingBox" : "useSpaceOnUse",
                "objectBoundingBox"
                );
        
        for (int i=0; i < stopOffsets.length; i++) {
            IXMLElement stop = new XMLElement("stop");
            writeAttribute(stop, "offset", toNumber(stopOffsets[i]), null);
            writeAttribute(stop, "stop-color", toColor(stopColors[i]), null);
            writeAttribute(stop, "stop-opacity", toNumber(stopColors[i].getAlpha() / 255d), "1");
            elem.addChild(stop);
        }
        
        return elem;
    }
    
    protected IXMLElement createRadialGradient(IXMLElement doc,
            double cx, double cy, double r,
            double[] stopOffsets, Color[] stopColors,
            boolean isRelativeToFigureBounds) throws IOException 
    {
    	IXMLElement elem = doc.createElement("radialGradient");

        writeAttribute(elem, "cx", toNumber(cx), "0.5");
        writeAttribute(elem, "cy", toNumber(cy), "0.5");
        writeAttribute(elem, "r", toNumber(r), "0.5");
        writeAttribute(elem, "gradientUnits", 
                (isRelativeToFigureBounds) ? "objectBoundingBox" : "useSpaceOnUse",
                "objectBoundingBox"
                );
        
        for (int i=0; i < stopOffsets.length; i++)
        {
            IXMLElement stop = new XMLElement("stop");
            writeAttribute(stop, "offset", toNumber(stopOffsets[i]), null);
            writeAttribute(stop, "stop-color", toColor(stopColors[i]), null);
            writeAttribute(stop, "stop-opacity", toNumber(stopColors[i].getAlpha() / 255d), "1");
            elem.addChild(stop);
        }
        
        return elem;
    }

    
    
	
	
}


