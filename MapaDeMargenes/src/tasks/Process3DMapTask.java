package tasks;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.JenksNaturalBreaksFunction;
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory2;
//import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.geotools.filter.function.RangedClassifier;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.stage.Screen;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import dao.CosechaItem;
import dao.FeatureContainer;
import dao.Producto;

public abstract class Process3DMapTask extends Task<Group>{
	protected Group map = null;//new Group();
	protected Quadtree featureTree;
	protected FileDataStore store = null;
	protected ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();
	public static Color[] colors = {
		Color.rgb(158,1,66),
		Color.rgb(213,62,79),
		Color. rgb(244,109,67),
		Color. rgb(253,174,97),
		Color. rgb(254,224,139),
		Color. rgb(255,255,191),
		Color. rgb(230,245,152),
		Color. rgb(171,221,164),
		Color.rgb(102,194,165),
		Color.rgb(50,136,189),
		Color.DARKBLUE};
	//	Color.rgb(94,79,162)};
	private static Double[] histograma=null;// es static para poder hacer constructHistograma static para usarlo en el grafico de Histograma
	public static Classifier clasifier=null;

	public static Producto producto;

	protected int featureCount;
	protected int featureNumber;


	@Override
	protected Group call() throws Exception {
		try {
			doProcess();
		} catch (Exception e1) {
			System.err.println("Error al procesar el Shape de cosechas");
			e1.printStackTrace();
		}

		return map;
	}

	protected abstract void doProcess()throws IOException ;

	protected abstract int getAmountMin() ;
	protected abstract int gerAmountMax() ;

	private Paint getColorFor(Double rinde) {	
		return colors[getCategoryFor(rinde)];
	}

	public static Integer getCategoryFor(Double rinde) {				
		if(histograma != null){
			return getColorByHistogram(rinde, histograma);
		} else if(clasifier != null){
			return getColorByJenks(rinde);
		}
		System.err.println("Error no hay un clasificador seleccionado");
		return null;
		//return getColorByHue(rinde, rindeMin, rindeMax, porcent);
	}

	public static String getCategoryNameFor(int index) {		
		String rangoIni = null;
		if(histograma != null){
			Double delta = histograma[1]-histograma[0];

			if(index<histograma.length){
				rangoIni = (histograma[index]-delta)+".."+histograma[index];
			}else {
				rangoIni = histograma[index]+".."+(histograma[index]+delta);
			}
		} else if(clasifier != null){			
			rangoIni = clasifier.getTitle(index);		
		}

		String [] partesIni = rangoIni.split("\\.\\.");
		DecimalFormat df = new DecimalFormat("0.00");
		return df.format(new Double(partesIni[0]))+"~"+df.format(new Double(partesIni[1]));// +"-"+histograma[j+1];
		
//		System.err.println("Error no hay un clasificador seleccionado");
//		return label;
	}

	private static int getColorByHistogram(Double rinde, Double[] histo) {
		int colorIndex = histo.length-1;
		try {
			BigDecimal bd = new BigDecimal(rinde);//java.lang.NumberFormatException: Infinite or NaN
			bd = bd.setScale(2, RoundingMode.HALF_UP);
			rinde = bd.doubleValue();
			for (int i = histo.length-1; i > -1 ; i--) {
				double histoMax = histo[i];
				if (rinde <= histoMax) {
					colorIndex = i;
				}
			}

			//	 System.out.println("Histograma color Index for rinde "+rinde+" is "+colorIndex);
			return colorIndex;
		} catch (Exception e) {
			System.err.println("getColorsByHistogram");
			e.printStackTrace();
			return 0;
		}
	}


	public static Classifier constructJenksClasifier(SimpleFeatureCollection collection,String amountColumn){
		//JenksFunctionTest test = new JenksFunctionTest("jenksTest");

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

		Literal classes = ff.literal(colors.length);
		PropertyName expr = ff.property(amountColumn);
		JenksNaturalBreaksFunction func = (JenksNaturalBreaksFunction) ff.function("Jenks", expr,
				classes);

		if(collection.size()>0){
			System.out.println("evaliando la colleccion para poder hacer jenkins");
			clasifier = (Classifier) func.evaluate(collection);//XXX esto demora unos segundos!
		} else{
			System.out.println("no se pudo evaluar jenkins porque la coleccion de datos es de tama�o cero");
		}
		if(clasifier == null){
			System.out.println("No se pudo evaluar la colleccion de features con el metodo de Jenkins");

		}
		//  int clase =   clasifier.classify(arg0)
		return clasifier;
	}

	private static int getColorByJenks(Double double1) {


		try{
			int colorIndex = clasifier.classify(double1);
			//	System.out.println("el color de jenks es: "+colorIndex);
			return colorIndex;
		}catch(Exception e){
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * 
	 * @param elementos Lista de Dao ordenados por getAmount() de menor a mayor
	 * @return 
	 */
	public static Double[] constructHistogram(List<? extends FeatureContainer> elementosItem){
		//1 ordeno los elementos de menor a mayor
		//2 bsuco el i*size/12 elemento y anoto si amount en la posicion i del vector de rangos

		//elementos.sort((e1, e2) -> e1.getAmount().compareTo(e2.getAmount()));//sorg ascending

		List<FeatureContainer> elementos = new LinkedList<FeatureContainer>(elementosItem);

		//Collections.sort(elementos);	
		System.out.println("termine de ordenar los elementos en constructHistogram");
		histograma=new Double[colors.length];
		if(elementos.size()>colors.length){
			//			Double rindeMin=elementos.get(0).getAmount();
			//			Double maxAmount = elementos.get(elementos.size()-1).getAmount();				

			Double rindeMin=0.0;
			Double maxAmount = 20.0;	
			if(producto!=null){
				maxAmount = producto.getRindeEsperado().getValue()*1.5;
			}

			Double deltaForColour =(maxAmount-rindeMin)/colors.length;

			for(int i = 0;i<colors.length;i++){	
				histograma[i]=rindeMin+deltaForColour*(i+1);
			}


		} else if(elementos.size()>0){
			Double rindeMin=elementos.get(0).getAmount();
			Double rindeMax=elementos.get(elementos.size()-1).getAmount();
			double rango= rindeMax-rindeMin;

			double delta = rango/colors.length;// si rango es cero delta es cero

			for(int i = 0;i<colors.length;i++){		
				histograma[i]=rindeMin+delta*i;
			}	
		}	
		//		System.out.print("histograma [");
		//		for(Double histo:histograma){
		//			System.out.print(histo+",");
		//		}
		//		System.out.println("]");
		//		
		//		ArrayList<Double> doubles = new ArrayList<Double>();
		//		for(Dao dao : elementos){
		//			doubles.add(dao.getAmount());
		//		}
		//		Double[] jenks = NaturalBreaks.getJenksBreaks(doubles, colors.length);
		//		System.out.println("jenks array " );
		//		
		//		System.out.print("jenks array [");
		//		for(Double jen:jenks){
		//			System.out.print(jen+",");
		//		}
		//		System.out.println("]");
		//		
		//		histograma=jenks;
		return histograma;
	}

	private Paint getColorByHue(Double rinde, int rindeMin, int rindeMax,
			double porcent) {
		double redHue = Color.RED.getHue();
		double greenHue = Color.GREEN.getHue();

		double colorHue = 0;
		double brightness= Color.BLACK.getBrightness();
		if(rinde<= rindeMax && rinde>=rindeMin){
			colorHue = redHue+porcent*(greenHue-redHue);

			brightness=Color.WHITE.getBrightness()+porcent*(Color.BLACK.getBrightness()-Color.WHITE.getBrightness());
		} else{
			System.out.println("rinde fuera de rango "+rinde);
		}

		return 	Color.hsb(colorHue, Color.RED.getSaturation(), brightness);
	}


	protected Path getPathFromGeom(Geometry poly, FeatureContainer dao) {			
		Path path = new Path();		
		/**
		 * recorro el vector de puntos que contiene el poligono gis y creo un
		 * path para dibujarlo
		 */
		if(poly.getNumPoints()==0){
			System.err.println("dibujando un path con cero puntos "+ poly);
			return null;
		}
		for (int i = 0; i < poly.getNumPoints(); i++) {
			Coordinate coord = poly.getCoordinates()[i];
			// como las coordenadas estan en long/lat las convierto a metros
			// para dibujarlas con menos error.
			double x = coord.x / ProyectionConstants.metersToLongLat;
			double y = coord.y /ProyectionConstants.metersToLongLat;
			if (i == 0) {
				path.getElements().add(new MoveTo(x, y)); // primero muevo el
			}
			path.getElements().add(new LineTo(x, y));// dibujo una linea desde
		}

		Paint currentColor = null;
		try{
		currentColor = getColorFor(dao.getAmount());
		}catch(NumberFormatException e){
			e.printStackTrace();
			currentColor = Color.WHITE;
		}


		//		if(Color.BLACK.equals(currentColor)){
		//			System.out.println("Infinite or NaN "+((Cosecha)dao).getId());
		//		}
		path.setFill(currentColor);
		path.setStrokeWidth(0.05);
		//		path.setCache(false);
		path.getStyleClass().add(currentColor.toString());//esto me permite luego asignar un estilo a todos los objetos con la clase "currentColor.toString()"
		//path.setStyle("-fx-fill: white;");//ok anda
		//		String pathClass=null;
		//		if(pathClass==null){
		//			pathClass = currentColor.toString();
		//			System.out.println("pathClass = "+pathClass);
		//			map.setStyle("0xe6f598ff.{"
		//				    +"-fx-fill: white;"
		//				+"}");
		//}
		/*
		 * pathClass = 0x3288bdff
pathClass = 0x3288bdff
pathClass = 0x3288bdff
pathClass = 0x66c2a5ff
pathClass = 0xe6f598ff
pathClass = 0xffffbfff
pathClass = 0xfee08bff
pathClass = 0xf46d43ff
pathClass = 0xd53e4fff
pathClass = 0x9e0142ff
		 */


		return path;
	}

	protected List<Polygon> getPolygons(FeatureContainer dao){

		List<Polygon> polygons = new ArrayList<Polygon>();
		Object geometry = dao.getGeometry();
		System.out.println("obteniendo los poligonos de "+geometry);

		if (geometry instanceof MultiPolygon) {		
			MultiPolygon mp = (MultiPolygon) geometry;
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				Geometry g = mp.getGeometryN(i);
				if(g instanceof Polygon){
					polygons.add((Polygon) g);
				}				
			}

		} else if (geometry instanceof Polygon) {
			polygons.add((Polygon) geometry);
		} else{
			System.out.println("me perdi en getPolygons "+geometry);//Las geometrias son POINT. que hago?
		}
		//System.out.println("devolviendo los polygons "+polygons);
		return polygons;
	}

	protected void runLater() {	


		ArrayList<Path> pathsToAdd = new ArrayList<Path>();

		//				Screen screen = Screen.getPrimary();
		//				Rectangle2D bounds = screen.getVisualBounds();
		//				
		//				Canvas canvas = new Canvas(bounds.getWidth()*2,bounds.getHeight()*2);
		//				GraphicsContext gc = canvas.getGraphicsContext2D();

		//	gc.fillRoundRect(500, 60, 30, 30, 10, 10);


		//6898688,-6898688
		//				MoveTo mo = (MoveTo)((Path) pathTooltips.get(0).get(0)).getElements().get(0);
		//				    MoveTo mt0 = new MoveTo(mo.getX()-500,mo.getY()-300);//(MoveTo)((Path) pathTooltips.get(0).get(0)).getElements().get(0);

		int i =0;
		for (ArrayList<Object> pathTooltip : pathTooltips) {
			updateProgress(i, pathTooltips.size());
			i++;
			Path path = (Path) pathTooltip.get(0);
			String tooltipText = (String) pathTooltip.get(1);

			pathsToAdd.add(path);

			//					gc.setFill(path.getFill());
			//					gc.setStroke(path.getStroke());
			//					
			//					ObservableList<PathElement> elements = path.getElements();
			//					
			//					gc.beginPath();
			//					for(PathElement pe : elements){
			//						if(pe instanceof MoveTo){
			//						
			//							MoveTo mt = (MoveTo) pe;
			//							System.out.println("moveTo "+mt);
			//							gc.moveTo(mt.getX()-mt0.getX(), mt.getY()-mt0.getY());
			//						//	gc.fillRoundRect(mt.getX()-mt0.getX(), mt.getY()-mt0.getY(), 30, 30, 0, 0);
			//						}else if(pe instanceof LineTo) {
			//							LineTo lt = (LineTo) pe;
			//							gc.lineTo(lt.getX()-mt0.getX(), lt.getY()-mt0.getY());
			//							System.out.println("lineTo "+lt);	
			//						}						
			//					}
			//					gc.closePath();
			//					gc.fill();
			//	gc.stroke();


			//		path.setStyle("-fx-fill: white;");

			//	path.setStyle(".white{-fx-fill: white;}");

			path.setOnMouseEntered(e -> {

				Tooltip t = new Tooltip(tooltipText);
				Font f = t.getFont();

				t.setFont(new Font(f.getName(), 32));//FIXME seguro esto me proboca el error del tama�o del texto
				Tooltip.install(path, t);

			});						

		}

		//				 gc.setFill(Color.GREEN);
		//			        gc.setStroke(Color.BLUE);
		//			        gc.setLineWidth(5);
		//				
		//				gc.beginPath();							
		//						gc.moveTo(20, 20);					
		//						gc.lineTo(100,20);		
		//						gc.lineTo(100,100);
		//						gc.lineTo(20,100);
		//						gc.lineTo(20,100);
		//				gc.closePath();
		//				gc.stroke();
		//				
		//				 gc.beginPath();
		//				    gc.moveTo(50, 50);
		//				    gc.bezierCurveTo(150, 20, 150, 150, 75, 150);
		//				 gc.closePath();
		//				 
		//				 gc.fillPolygon(new double[]{10, 40, 10, 40},
		//	                       new double[]{210, 210, 240, 240}, 4);
		//				
		//				gc.fillRoundRect(100, 100, 30, 30, 0, 0);
		//			Platform.runLater(new Runnable() {


		//				@Override
		//				public void run() {
		//	map.getChildren().add(canvas);
		map.getChildren().addAll(pathsToAdd);//antes tenia setAll que es mas rapido pero me borraba los nodos que habia antes si los habia

		//	map.getChildren().setAll(pathsToAdd);//antes tenia setAll que es mas rapido pero me borraba los nodos que habia antes si los habia

		//				SnapshotParameters params = new SnapshotParameters();
		//				params.setFill(Color.TRANSPARENT);
		//				
		//				  WritableImage image = map.snapshot(params, null);
		//				  gc.drawImage(image, 0, 0);
		//	  map.getChildren().clear();
		//	map.getChildren().add(canvas);


		//		}
		//	});

	}

	//	protected void canvasRunLater() {		
	//		Screen screen = Screen.getPrimary();
	//		Rectangle2D bounds = screen.getVisualBounds();
	//		
	//		Canvas canvas = new Canvas(bounds.getWidth()*2,bounds.getHeight()*2);
	//		GraphicsContext gc = canvas.getGraphicsContext2D();		
	//		                           //6898688,-6898688
	//		MoveTo mo = (MoveTo) ((Path) pathTooltips.get(0).get(0)).getElements()
	//				.get(0);
	//		MoveTo mt0 = new MoveTo(mo.getX() - 500, mo.getY() - 300);// (MoveTo)((Path)
	//																	// pathTooltips.get(0).get(0)).getElements().get(0);
	//
	//		int i = 0;
	//		for (ArrayList<Object> pathTooltip : pathTooltips) {
	//			updateProgress(i, pathTooltips.size());
	//			i++;
	//			Path path = (Path) pathTooltip.get(0);
	//
	//			gc.setFill(path.getFill());
	//			gc.setStroke(path.getStroke());
	//
	//			ObservableList<PathElement> elements = path.getElements();
	//
	//			gc.beginPath();
	//			for (int j =0; j<elements.size();j++ ) {
	//				PathElement pe = elements.get(j);
	//				if (j==0) {
	//					MoveTo mt = (MoveTo) pe;
	//					gc.moveTo(mt.getX() - mt0.getX(), mt.getY() - mt0.getY());
	//
	//				} else  {
	//					LineTo lt = (LineTo) pe;
	//					gc.lineTo(lt.getX() - mt0.getX(), lt.getY() - mt0.getY());
	//				}
	//			}
	//			gc.closePath();
	//			gc.fill();
	//
	//		}
	//
	//		Platform.runLater(new Runnable() {
	//			@Override
	//			public void run() {
	//				map.getChildren().add(canvas);
	//			}
	//		});
	//	}

}

