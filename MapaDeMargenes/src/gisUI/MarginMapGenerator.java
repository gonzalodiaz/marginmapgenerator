package gisUI;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

import javax.imageio.ImageIO;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;

import tasks.CannyEdgeDetector;
import tasks.HarvestFiltersConfig;
import tasks.LSDetector;
import tasks.ProcessFertMapTask;
import tasks.ProcessGroupsMapTask;
import tasks.ProcessHarvest3DMapTask;
import tasks.ProcessHarvestMapTask;
import tasks.ProcessMapTask;
import tasks.ProcessMarginMapTask;
import tasks.ProcessNewSoilMapTask;
import tasks.ProcessPulvMapTask;
import tasks.ProcessSiembraMapTask;
import tasks.ProcessSoilMapTask;

import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Configuracion;
import dao.CosechaItem;
import dao.Costos;
import dao.FertilizacionItem;
import dao.Fertilizante;
import dao.Producto;
import dao.PulverizacionItem;
import dao.RentabilidadItem;
import dao.Siembra;
import dao.Suelo;

public class MarginMapGenerator extends Application {


	private static final double DIVIDER_POSITION = 0.9;

	private static final String TITLE_VERSION = "Economia de Precision (Margin Map Viewer Ver: 0.1.23)";

	//private static final String ICON = "gisUI/octopus_1.png";
	//private static final String ICON = "gisUI/images (2).jpg";
	//private static final String ICON = "gisUI/mapa de rentabilidades.png";
	//private static final String ICON = "gisUI/farm1.png";
	private static final String ICON = "gisUI/1-512.png";

//	private static final long SCROLL_LIMIT_TIME = 1000;
	Group root = new Group();
	Group map = new Group();
	Scene scene;
	private Stage stage;
	//private GoogleMapView mapView;

	Group fertMap = new Group();
	Group siembraMap = new Group();
	Group pulvMap = new Group();
	Group harvestMap = new Group();
	Group marginMap = new Group();
	Group suelosMap = new Group();
	Group newSoilMap = new Group();

	private VBox progressBox = new VBox();



//	private double dragBaseX, dragBaseY;
//	private double dragBase2X, dragBase2Y;
	// private GoogleMapView mapView;
	private ScrollBar timeline = new ScrollBar();;



	private Quadtree pulvTree;
	private Quadtree harvestTree;
	private Quadtree siembraTree;
	private Quadtree sueloTree;
	private Quadtree fertTree;
	private Quadtree rentaTree;

//	private File file;


	SplitPane horizontalSplit = new SplitPane();
	ImageView iv1 = new ImageView();
	ImageView iv2 = new ImageView();

	private Producto producto;
	private Fertilizante fertilizante;
	//private StringProperty consoleText;



	public static void main(String[] args) {
		Application.launch(MarginMapGenerator.class, args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.stage=primaryStage;
		primaryStage.setTitle(TITLE_VERSION);
		primaryStage.getIcons().add(new Image(ICON));

		//	primaryStage.setMaximized(true);

		Screen screen = Screen.getPrimary();
		Rectangle2D bounds = screen.getVisualBounds();
		//		primaryStage.setMaxHeight(bounds.getHeight()*2/3);
		//		primaryStage.setMaxWidth(bounds.getWidth()*2/3);

		scene = new Scene(root,bounds.getWidth()*9/10,bounds.getHeight()*9/10);//, 1500, 800);//, Color.White);
		primaryStage.setScene(scene);

		scene.getStylesheets().add("gisUI/style.css");//esto funciona

		map.setCache(true);// true significa que no va a redibujar los nodos
		map.setCacheHint(CacheHint.SCALE_AND_ROTATE);

		resetMapScale();
		map.getChildren().add(suelosMap);
		map.getChildren().add(fertMap);
		map.getChildren().add(siembraMap);
		map.getChildren().add(pulvMap);
		map.getChildren().add(harvestMap);
		map.getChildren().add(marginMap);
		
		map.getChildren().add(newSoilMap);
		
		
		iv1.setScaleY(-1);
		iv2.setScaleY(-1);
//		
		//XXX si agrego las imagenes de esta forma quedan sobre los mapas y no se los ve
//		this.map.getChildren().add(iv1);
//		this.map.getChildren().add(iv2);

		//		mapView = new GoogleMapView();
		//		mapView.addMapInializedListener(this);
		//		mapView.scaleYProperty().set(-1);
		//		mapView.setPrefSize(1500, 800);
		//		map.getChildren().add(mapView);


		MenuBar menuBar = constructMenuBar();
		Parent zoomPane = createZoomPane(map);

		StackPane bp = new StackPane();		
		bp.getChildren().add(zoomPane);
		StackPane.setAlignment(zoomPane, Pos.TOP_RIGHT);

		VBox vBox1 = new VBox();
		vBox1.getChildren().addAll(menuBar,bp);

		horizontalSplit.setOrientation(Orientation.VERTICAL);
		horizontalSplit.setDividerPositions(DIVIDER_POSITION);
		ListView<String> console = new ListView<String>();		
		installConsole(console);

		horizontalSplit.getItems().addAll(vBox1,console);
		root.getChildren().addAll(horizontalSplit);
		primaryStage.show();
	}

	private void installConsole(ListView<String> console) {
		PrintStream ps = new PrintStream(System.out, true){
//XXX super.println llama a print asi que no hace falta sobreescribirlo
//			@Override
//			public void println(String s){
//				super.println(s);				
////				Platform.runLater(()-> {
////					console.getItems().add(0, s);
////
////				});
//
//			}
			
			@Override
			public void print(String s){
					
				Platform.runLater(()-> {
					super.print(s);		
					console.getItems().add(0, s);

				});

			}
		};		
		System.setOut(ps);

		PrintStream pse = new PrintStream(System.err, true){
			
//			@Override
//			public void println(String s){
//				super.println(s);
//
//				Platform.runLater(()-> {
//					console.getItems().add(0, s);
//
//				});
//
//			}
			
			@Override
			public void print(String s){
					
				Platform.runLater(()-> {
					super.print(s);			
					console.getItems().add(0, s);

				});

			}
		};		
		System.setErr(pse);
	}

	private MenuBar constructMenuBar() {
		final Menu menuImportar = new Menu("Importar");
		final Menu menuExportar = new Menu("Exportar");
		final Menu menuVer = new Menu("Capas");
		final Menu menuImgs = new Menu("Imagenes");

		final Menu menuEstadisticas = new Menu("Estadisticas");

		final Menu menuConfiguracion = new Menu("Configuracion");
		MenuItem menuItemProductos = new MenuItem("Productos");
		menuItemProductos.setOnAction(a->doShowABMProductos());
		menuConfiguracion.getItems().add(menuItemProductos);
		menuConfiguracion.getItems().add(new CustomMenuItem(constructDatosPane()));
	
		MenuItem menuItemSuelo = new MenuItem("Suelo");
		menuItemSuelo.setOnAction(a->doOpenSoilMap());
		menuImportar.getItems().add(menuItemSuelo);
		
		MenuItem menuItemFertilizacion = new MenuItem("Fertilizacion");
		menuItemFertilizacion.setOnAction(a->doOpenFertMap());
		menuImportar.getItems().add(menuItemFertilizacion);


		MenuItem menuItemSiembra = new MenuItem("Siembra");
		menuItemSiembra.setOnAction(a->doOpenSiembraMap());
		menuImportar.getItems().add(menuItemSiembra);

		MenuItem menuItemPulverizacion = new MenuItem("Pulverizacion");
		menuItemPulverizacion.setOnAction(a->doOpenPulvMap());
		menuImportar.getItems().add(menuItemPulverizacion);

		MenuItem menuItemCosecha = new MenuItem("Cosecha");
		menuItemCosecha.setOnAction(a->doOpenHarvestMap());
		menuImportar.getItems().add(menuItemCosecha);


		MenuItem menuItemRentabilidad = new MenuItem("Retabilidades");
		menuItemRentabilidad.setOnAction(a->doProcessMargin());
		menuImportar.getItems().add(menuItemRentabilidad);
		
		MenuItem menuCosecha3D = new MenuItem("Cosecha3D");
		menuCosecha3D.setOnAction(a->doCosecha3D());
		menuImportar.getItems().add(menuCosecha3D);
		

		MenuItem menuItemGrupos = new MenuItem("Grupos");
		menuItemGrupos.setOnAction(a->doOpenGroupsMap());
		menuImportar.getItems().add(menuItemGrupos);
		

		MenuItem menuItemExportarCosecha = new MenuItem("Cosecha");
		menuItemExportarCosecha.setOnAction(a->doExportHarvest());
		menuExportar.getItems().add(menuItemExportarCosecha);

		MenuItem menuItemExportarRentabilidades = new MenuItem("Rentabilidades");
		menuItemExportarRentabilidades.setOnAction(a->doExportMargins());
		menuExportar.getItems().add(menuItemExportarRentabilidades);
		
		MenuItem menuItemExportarSuelo = new MenuItem("Suelo");
		menuItemExportarSuelo.setOnAction(a->doExportSuelo());
		menuExportar.getItems().add(menuItemExportarSuelo);

		MenuItem menuItemExportarCaptura= new MenuItem("Pantalla");
		menuItemExportarCaptura.setOnAction(a->doSnapshot());
		menuExportar.getItems().add(menuItemExportarCaptura);


		MenuItem menuObtenerBordes = new MenuItem("Obtener Bordes Canny");
		menuObtenerBordes.setOnAction(a->doShowBorders());
		menuImgs.getItems().add(menuObtenerBordes);
		
		MenuItem menuObtenerBordesLSD = new MenuItem("Obtener Bordes LSD");
		menuObtenerBordesLSD.setOnAction(a->doShowBordersLSD());
		menuImgs.getItems().add(menuObtenerBordesLSD);
		
		MenuItem menuObtenerBordesCombined = new MenuItem("Obtener Bordes Combined");
		menuObtenerBordesCombined.setOnAction(a->doShowBordersCombined());
		menuImgs.getItems().add(menuObtenerBordesCombined);


		MenuItem menuItemHistogramaCosecha = new MenuItem("Histograma Cosecha");
		menuItemHistogramaCosecha.setOnAction(a->showHistoCosecha());
		menuEstadisticas.getItems().add(menuItemHistogramaCosecha);



		CheckMenuItem showFertMenuItem = new CheckMenuItem("Fertilizacion");
		fertMap.visibleProperty().bindBidirectional(showFertMenuItem.selectedProperty());

		CheckMenuItem showSiembraMenuItem = new CheckMenuItem("Siembra");
		siembraMap.visibleProperty().bindBidirectional(showSiembraMenuItem.selectedProperty());

		CheckMenuItem showPulvMenuItem = new CheckMenuItem("Pulverizacion");
		pulvMap.visibleProperty().bindBidirectional(showPulvMenuItem.selectedProperty());

		CheckMenuItem showHarvestMenuItem = new CheckMenuItem("Cosecha");
		harvestMap.visibleProperty().bindBidirectional(showHarvestMenuItem.selectedProperty());

		CheckMenuItem showMarginMenuItem = new CheckMenuItem("Retabilidades");
		marginMap.visibleProperty().bindBidirectional(showMarginMenuItem.selectedProperty());		

		CheckMenuItem showSoilMI = new CheckMenuItem("Suelo");
		suelosMap.visibleProperty().bindBidirectional(showSoilMI.selectedProperty());	
		
		CheckMenuItem showNewSoilMI = new CheckMenuItem("Nuevo Suelo");
		newSoilMap.visibleProperty().bindBidirectional(showNewSoilMI.selectedProperty());		


		menuVer.getItems().addAll(showSoilMI,showFertMenuItem, 
				showSiembraMenuItem, showPulvMenuItem,
				showHarvestMenuItem, showMarginMenuItem,showNewSoilMI);

		Configuracion config = Configuracion.getInstance();
		HarvestFiltersConfig filtersConfig = HarvestFiltersConfig.getInstance();
		CheckMenuItem corregirAncho = new CheckMenuItem("Ancho");		
		corregirAncho.selectedProperty().bindBidirectional(filtersConfig.correccionAnchoProperty());

		CheckMenuItem corregirDistancia = new CheckMenuItem("Distancia");
		corregirDistancia.selectedProperty().bindBidirectional(filtersConfig.correccionDistanciaProperty());

		CheckMenuItem corregirSuperposicion = new CheckMenuItem("Superposicion");
		corregirSuperposicion.selectedProperty().bindBidirectional(filtersConfig.correccionSuperposicionProperty());

		CheckMenuItem corregirRinde = new CheckMenuItem("Rinde");
		corregirRinde.selectedProperty().bindBidirectional(filtersConfig.correccionRindeProperty());

		CheckMenuItem corregirOutlayers = new CheckMenuItem("Outlayers");
		corregirOutlayers.selectedProperty().bindBidirectional(filtersConfig.correccionOutlayersProperty());

		CheckMenuItem corregirDemora = new CheckMenuItem("Demora");
		corregirDemora.selectedProperty().bindBidirectional(filtersConfig.correccionDemoraProperty());

		CheckMenuItem generarRentabilidadFromShp = new CheckMenuItem("Shp Rentabilidad");
		generarRentabilidadFromShp.selectedProperty().bindBidirectional(config.generarMapaRentabilidadFromShpProperty());

		Menu harvestFilters = new Menu("Filtros Cosecha");
		harvestFilters.getItems().addAll(corregirAncho,corregirDistancia,corregirSuperposicion,corregirRinde,corregirOutlayers,corregirDemora,generarRentabilidadFromShp);
		menuConfiguracion.getItems().add(harvestFilters);

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menuImportar, menuExportar, menuVer,menuImgs,menuEstadisticas,menuConfiguracion);
		menuBar.setPrefWidth(scene.getWidth());
		return menuBar;
	}



	private void doShowABMProductos() {
		// TODO Auto-generated method stub

	}

	private void doOpenSoilMap() {
		FileDataStore store = chooseShapeFileAndGetStore();
		if (store != null) {
			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */
			List<String> availableColumns = getAvailableColumns(store);

			ColumnSelectDialog csd = new ColumnSelectDialog(
					Suelo.getRequiredColumns(), availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();

				Suelo.setColumnsMap(columns);
				System.out.println("columns map: " + columns);
			} else {
				System.out.println("columns names not set");
			}

			// // The Java 8 way to get the response value (with lambda
			// expression).
			// result.ifPresent(letter -> System.out.println("Your choice: " +
			// letter));

			/**/
			//map.getChildren().remove(fertMap);
			this.suelosMap.getChildren().clear();
			resetMapScale();

	

	
			Group fgroup = new Group();
			ProcessSoilMapTask pfMapTask = new ProcessSoilMapTask(
					fgroup,store);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(
					pfMapTask.progressProperty());
			Thread currentTaskThread = new Thread(pfMapTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();

			pfMapTask.setOnSucceeded(handler -> {
				this.sueloTree = (Quadtree) handler.getSource().getValue();
				suelosMap.getChildren().add(fgroup);
				Bounds bl = suelosMap.getBoundsInLocal();
				System.out.println("bounds de siembraMap es: " + bl);
				//				fertMap.setLayoutX(-bl.getMinX());
				//				fertMap.setLayoutY(-bl.getMinY());

				//		map.getChildren().add(fertMap);
				suelosMap.visibleProperty().set(true);
				// Group taskMap = (Group) handler.getSource().getValue();
				System.out.println("OpenSoilMapTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
		}
	}
	/**
	 * generar un shp a partir del mapa de suelos anterior, la fertilizacion y la extraccion de nutrientes estimada por el cultivo
	 */
	private void doExportSuelo() {
		FileDataStore store = chooseShapeFileAndGetStore();
		if (store != null) {

			System.out.println("exportNewSoilMap");
			
			//map.getChildren().remove(marginMap);
			newSoilMap.getChildren().clear();
			resetMapScale();
			Group mGroup = new Group();
			ProcessNewSoilMapTask uMmTask = new ProcessNewSoilMapTask(store, mGroup,sueloTree
					, fertTree,harvestTree , producto, fertilizante);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(uMmTask.progressProperty());
			Thread currentTaskThread = new Thread(uMmTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();
			
			
			uMmTask.setOnSucceeded(handler -> {
				Quadtree newSoilTree = (Quadtree)	handler.getSource().getValue();
				newSoilMap.getChildren().add(mGroup);
				
				Bounds bl = marginMap.getBoundsInLocal();
				System.out.println("bounds de marginMap es: " + bl);
				//			marginMap.setLayoutX(-bl.getMinX());
				//			marginMap.setLayoutY(-bl.getMinY());
				//	map.getChildren().add( marginMap);
				newSoilMap.visibleProperty().set(true);

				System.out.println("ProcessMarginTask succeded");
				progressBox.getChildren().remove(progressBarTask);
				exportarAShp(newSoilTree);		
			});
		}
		
		
		
		
	}

	public void exportarAShp(Quadtree newSoilTree) {
		@SuppressWarnings("unchecked")
		List<Suelo> cosechas = newSoilTree.queryAll();
		//	System.out.println("construyendo el shp para las rentas "+rentas.size());
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
				Suelo.getType());
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();

		for (Suelo cosecha : cosechas) {
			SimpleFeature cosechaFeature = cosecha.getFeature(featureBuilder);
			features.add(cosechaFeature);
			//	System.out.println("agregando a features "+rentaFeature);

		}

		File shapeFile =  getNewShapeFile();

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		try {
			params.put("url", shapeFile.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		params.put("create spatial index", Boolean.TRUE);


		ShapefileDataStore newDataStore=null;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(Suelo.getType());

			//		System.out.println("antes de forzar wgs 84");

			/*
			 * You can comment out this line if you are using the createFeatureType
			 * method (at end of class file) rather than DataUtilities.createType
			 */
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String typeName = newDataStore.getTypeNames()[0];
		//	System.out.println("typeName 0 del newDataStore es "+typeName);
		SimpleFeatureSource featureSource = null;
		try {
			featureSource = newDataStore.getFeatureSource(typeName);
			//	System.out.println("cree new featureSource "+featureSource.getInfo());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (featureSource instanceof SimpleFeatureStore) {			

			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			Transaction transaction = new DefaultTransaction("create");
			featureStore.setTransaction(transaction);

			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */
			SimpleFeatureCollection collection = new ListFeatureCollection(CosechaItem.getType(), features);
			//	System.out.println("agregando features al store " +collection.size());
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
				//	System.out.println("commiting transaction "+ featureStore.getCount(new Query()));
			} catch (Exception problem) {
				problem.printStackTrace();
				try {
					transaction.rollback();
					//	System.out.println("transaction rolledback");
				} catch (IOException e) {

					e.printStackTrace();
				}

			} finally {
				try {
					transaction.close();
					//System.out.println("closing transaction");
				} catch (IOException e) {

					e.printStackTrace();
				}
			}

		}
	}


	

	private void doShowBordersCombined() {
		Image image =null;
		File imageFile = chooseFiles("JPG","*.jpg").get(0);

		if (imageFile != null) {			
			try {
				image = new Image(new FileInputStream(imageFile));							 
				System.out.println("Mostrando Imagen");
				iv1.setImage(image);
				iv1.setOpacity(1);
				//this.map.getChildren().add(iv1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else { 
			System.out.println("image file es null "+imageFile);
		}
		

		CannyEdgeDetector detector = new CannyEdgeDetector();
		//adjust its parameters as desired
		//TODO mover estos parametros al config.ini
		detector.setLowThreshold(0.25f);
		detector.setHighThreshold(0.3f);
		detector.setRoofThreshold(0.5f);
		detector.setGaussianKernelRadius(10);//10 capta bien los surcos
		detector.setGaussianKernelWidth(50);

		//apply it to an image
		detector.setSourceImage(SwingFXUtils.fromFXImage(image, null));
		
		ProgressBar progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);
		progressBarTask.progressProperty().bind(detector.progressProperty());
		progressBox.getChildren().add(progressBarTask);
		detector.setOnSucceeded(handler -> {
			progressBox.getChildren().remove(progressBarTask);
			BufferedImage edgesBI =( BufferedImage) handler.getSource().getValue();
			Image edges = SwingFXUtils.toFXImage(edgesBI, null);
			System.out.println("Mostrando Edges");
		
			iv2.setImage(edges);
			iv2.setOpacity(0.5);
		//	map.getChildren().add(iv2);
			
			
			
			LSDetector lSDdetector = new LSDetector();


			//apply it to an image
			lSDdetector.setSourceImage(SwingFXUtils.fromFXImage(iv2.getImage(), null));
			
			ProgressBar lSDprogressBarTask = new ProgressBar();			
			lSDprogressBarTask.setProgress(0);
			lSDprogressBarTask.progressProperty().bind(lSDdetector.progressProperty());
			progressBox.getChildren().add(lSDprogressBarTask);
			lSDdetector.setOnSucceeded(LSDhandler -> {
				progressBox.getChildren().remove(lSDprogressBarTask);
				BufferedImage LSDedgesBI =( BufferedImage) LSDhandler.getSource().getValue();
				Image LSDedges = SwingFXUtils.toFXImage(LSDedgesBI, null);
				System.out.println("Mostrando Edges");
			
				iv2.setImage(LSDedges);
				iv2.setOpacity(0.5);
			//	map.getChildren().add(iv2);
				
			});
			
			Thread lSDThread = new Thread(lSDdetector);
			lSDThread .setDaemon(true);
			lSDThread.start();
			
		});
		
		Thread cannyThread = new Thread(detector);
		cannyThread .setDaemon(true);
		cannyThread.start();
		
	
		

		

	}

	private void doShowBordersLSD() {
		Image image =null;
		File imageFile = chooseFiles("JPG","*.jpg").get(0);

		if (imageFile != null) {			
			try {
				image = new Image(new FileInputStream(imageFile));							 
				System.out.println("Mostrando Imagen");
				iv1.setImage(image);
				iv1.setOpacity(1);
				//this.map.getChildren().add(iv1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else { 
			System.out.println("image file es null "+imageFile);
		}
		

		LSDetector lSDdetector = new LSDetector();
		//adjust its parameters as desired
		//TODO mover estos parametros al config.ini
//		detector.setLowThreshold(0.25f);
//		detector.setHighThreshold(0.3f);
//		detector.setRoofThreshold(0.5f);
//		detector.setGaussianKernelRadius(10);//10 capta bien los surcos
//		detector.setGaussianKernelWidth(50);

		//apply it to an image
		lSDdetector.setSourceImage(SwingFXUtils.fromFXImage(image, null));
		
		ProgressBar lSDprogressBarTask = new ProgressBar();			
		lSDprogressBarTask.setProgress(0);
		lSDprogressBarTask.progressProperty().bind(lSDdetector.progressProperty());
		progressBox.getChildren().add(lSDprogressBarTask);
		lSDdetector.setOnSucceeded(handler -> {
			progressBox.getChildren().remove(lSDprogressBarTask);
			BufferedImage edgesBI =( BufferedImage) handler.getSource().getValue();
			Image edges = SwingFXUtils.toFXImage(edgesBI, null);
			System.out.println("Mostrando Edges");
		
			iv2.setImage(edges);
			iv2.setOpacity(0.5);
		//	map.getChildren().add(iv2);
			
		});
		
		Thread lSDThread = new Thread(lSDdetector);
		lSDThread .setDaemon(true);
		lSDThread.start();
	}

	private void doShowBorders() {
		Image image =null;
		File imageFile = chooseFiles("JPG","*.jpg").get(0);

		if (imageFile != null) {			
			try {
				image = new Image(new FileInputStream(imageFile));							 
				System.out.println("Mostrando Imagen");
				iv1.setImage(image);
				iv1.setOpacity(1);
				//this.map.getChildren().add(iv1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else { 
			System.out.println("image file es null "+imageFile);
		}
		

		CannyEdgeDetector detector = new CannyEdgeDetector();
		//adjust its parameters as desired
		//TODO mover estos parametros al config.ini
		detector.setLowThreshold(0.25f);
		detector.setHighThreshold(0.3f);
		detector.setRoofThreshold(0.5f);
		detector.setGaussianKernelRadius(10);//10 capta bien los surcos
		detector.setGaussianKernelWidth(50);

		//apply it to an image
		detector.setSourceImage(SwingFXUtils.fromFXImage(image, null));
		
		ProgressBar progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);
		progressBarTask.progressProperty().bind(detector.progressProperty());
		progressBox.getChildren().add(progressBarTask);
		detector.setOnSucceeded(handler -> {
			progressBox.getChildren().remove(progressBarTask);
			BufferedImage edgesBI =( BufferedImage) handler.getSource().getValue();
			Image edges = SwingFXUtils.toFXImage(edgesBI, null);
			System.out.println("Mostrando Edges");
		
			iv2.setImage(edges);
			iv2.setOpacity(0.5);
		//	map.getChildren().add(iv2);
			
		});
		
		Thread cannyThread = new Thread(detector);
		cannyThread .setDaemon(true);
		cannyThread.start();


	}

	private void showHistoCosecha() {	
	
		Task<CosechaHistoChart> pfMapTask = new Task<CosechaHistoChart>(){
			@Override
			protected CosechaHistoChart call() throws Exception {
				try{
				CosechaHistoChart histoChart = new CosechaHistoChart(harvestTree);		
				
				
				return histoChart;
				}catch(Throwable t){
					t.printStackTrace();
				}
				return null;
			}			
		};
		
		
		pfMapTask.setOnSucceeded(handler -> {
			CosechaHistoChart	histoChart = (CosechaHistoChart) handler.getSource().getValue();	
			Stage histoStage = new Stage();
			histoStage.setTitle("Histograma Cosecha");
			histoStage.getIcons().add(new Image(ICON));
			Scene scene = new Scene(histoChart, 800, 600);
			histoStage.setScene(scene);
			System.out.println("termine de crear el histo chart");
			histoStage.initOwner(this.stage);
			histoStage.show();
			System.out.println("histoChart.show();");
		});
		
		Thread currentTaskThread = new Thread(pfMapTask);
		currentTaskThread.setDaemon(true);
		currentTaskThread.start();
	}

	private Node constructDatosPane() {
		StringConverter<Number> converter = new NumberStringConverter();
	
		TextField precioGranoTf = new TextField();//11
		precioGranoTf.setPromptText("Precio grano");
		Bindings.bindBidirectional(precioGranoTf.textProperty(), Costos.getInstance().precioGranoProperty, converter);
		Label precioGranoLbl = new Label("Precio Grano");
		precioGranoLbl.setTextFill(Color.BLACK);

		TextField precioFertTf = new TextField();//"0.6"
		precioFertTf.setPromptText("El precio del fertilizante en $/kg");
		Bindings.bindBidirectional(precioFertTf.textProperty(), Costos.getInstance().precioFertProperty, converter);
		Label precioFertLbl = new Label("Precio Fertilizante");
		precioFertLbl.setTextFill(Color.BLACK);

		TextField precioLabFertTf = new TextField();//"12"
		precioLabFertTf.setPromptText("El precio de la labor de fertilizacion por Hectarea");
		Bindings.bindBidirectional(precioLabFertTf.textProperty(), Costos.getInstance().precioLabFertProperty, converter);
		Label precioLaborFertLbl = new Label("Costo Labor Fertilizacion");
		precioLaborFertLbl.setTextFill(Color.BLACK);

		TextField precioLaborPulvTf = new TextField();//"5.3"
		precioLaborPulvTf.setPromptText("El precio de la labor de pulverizacion por Hectarea");
		Bindings.bindBidirectional(precioLaborPulvTf.textProperty(), Costos.getInstance().precioPulvProperty, converter);
		Label precioLaborPulvLbl = new Label("Costo Labor Pulverizacion");
		precioLaborPulvLbl.setTextFill(Color.BLACK);

		TextField precioLaborSiembraTf = new TextField();//"50"
		precioLaborSiembraTf.setPromptText("El precio de la labor de siembra por Hectarea");
		Bindings.bindBidirectional(precioLaborSiembraTf.textProperty(), Costos.getInstance().precioSiembraProperty, converter);
		Label precioLaborSiembraLbl = new Label("Precio labor Siembra");
		precioLaborSiembraLbl.setTextFill(Color.BLACK);

		TextField precioSemillavTf = new TextField();//"170"
		precioSemillavTf.setPromptText("Precio bolsa semilla");
		Bindings.bindBidirectional(precioSemillavTf.textProperty(), Costos.getInstance().precioSemillaProperty, converter);
		Label precioSemillaLbl = new Label("Precio Bolsa Semilla");
		precioSemillaLbl.setTextFill(Color.BLACK);

		TextField correccionCosechaTf = new TextField("100");
		correccionCosechaTf.setPromptText("Correccion rinde");
		Bindings.bindBidirectional(correccionCosechaTf.textProperty(), Costos.getInstance().correccionCosechaProperty, converter);
		Label correccionCosechaLbl = new Label("Correccion Rinde");
		correccionCosechaLbl.setTextFill(Color.BLACK);
		
		TextField costoFijoHaTf = new TextField();//"170"
		costoFijoHaTf.setPromptText("Costo fijo por Hectarea");
		Bindings.bindBidirectional(costoFijoHaTf.textProperty(), Costos.getInstance().costoFijoHaProperty, converter);
		Label costoFijoHaLbl = new Label("Costo Fijo Ha");
		costoFijoHaLbl.setTextFill(Color.BLACK);

		timeline.setOrientation(Orientation.HORIZONTAL);
		timeline.setMin(0.0);

		timeline.setVisible(false);
		timeline.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				doTimelineScroll(old_val, new_val);
			}
		});

		
		ChoiceBox<Producto> productoCh=new ChoiceBox<Producto>();
		productoCh.getItems().setAll(Producto.productos.values());
		productoCh.getSelectionModel().selectedItemProperty().addListener(( ov, oldPeriodo,  newProducto) ->{
			this.producto=newProducto;
			ProcessMapTask.producto=newProducto;
		});
		productoCh.getSelectionModel().select(0);
	
			
		ChoiceBox<Fertilizante> fertilizanteCh=new ChoiceBox<Fertilizante>();
		fertilizanteCh.getItems().setAll(Fertilizante.fertilizantes);
		fertilizanteCh.getSelectionModel().selectedItemProperty().addListener(( ov, oldPeriodo,  newPeriodo) ->{
			this.fertilizante=newPeriodo;
		});
		fertilizanteCh.getSelectionModel().select(0);
	
	
			
		
		VBox gp = new VBox();
		gp.getChildren().add(fertilizanteCh);
		gp.getChildren().add(productoCh);
		gp.getChildren().add(precioFertLbl);
		gp.getChildren().add(precioFertTf);
		gp.getChildren().add(precioLaborFertLbl);
		gp.getChildren().add(precioLabFertTf);
		gp.getChildren().add(precioSemillaLbl);
		gp.getChildren().add(precioSemillavTf);
		gp.getChildren().add(precioLaborSiembraLbl);
		gp.getChildren().add(precioLaborSiembraTf);
		gp.getChildren().add(precioLaborPulvLbl);
		gp.getChildren().add(precioLaborPulvTf);
		gp.getChildren().add(precioGranoLbl);
		gp.getChildren().add(precioGranoTf);
		gp.getChildren().add(costoFijoHaLbl);
		gp.getChildren().add(costoFijoHaTf);
		gp.getChildren().add(correccionCosechaLbl);
		gp.getChildren().add(correccionCosechaTf);
		gp.getChildren().add(progressBox);// progressBarTask
		gp.getChildren().add(timeline);
		
//		Menu precios = new Menu("Precios");
//		precios.getItems().addAll(new CustomMenuItem(precioFertLbl),
//				new CustomMenuItem(precioFertTf),
//				new CustomMenuItem(precioLaborFertLbl),
//				new CustomMenuItem(precioLbFertTf),
//				new CustomMenuItem(precioSemillaLbl),
//				new CustomMenuItem(precioSemillavTf),
//				new CustomMenuItem(precioLaborSiembraLbl),
//				new CustomMenuItem(precioLaborSiembraTf),
//				new CustomMenuItem(precioLaborPulvLbl),
//				new CustomMenuItem(precioLaborPulvTf),
//				new CustomMenuItem(precioGranoLbl),
//				new CustomMenuItem(precioGranoTf),
//				new CustomMenuItem(costoFijoHaLbl),
//				new CustomMenuItem(costoFijoHaTf),
//				new CustomMenuItem(correccionCosechaLbl),
//				new CustomMenuItem(correccionCosechaTf),
//				new CustomMenuItem(progressBox),
//				new CustomMenuItem(timeline));
		
		return gp;
	}

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	private void doOpenHarvestMap() {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores();
		if (stores != null) {

			List<String> availableColumns = getAvailableColumns(stores.get(0));

			ColumnSelectDialog csd = new ColumnSelectDialog(
					CosechaItem.getRequieredColumns(), availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();
				CosechaItem.setColumnsMap(columns);
				//	System.out.println("columns map: " + columns);
			} else {
				//System.out.println("columns names not set");
				return;
			}

			//			map.getChildren().remove(harvestMap);

			harvestMap.getChildren().clear();
			resetMapScale();

			//			harvestMap.setCache(false);// true significa que no va a redibujar los nodos
			//	map.setCacheHint();
			//	map.setCacheHint(CacheHint.QUALITY);


			Double precioGrano = Costos.getInstance().precioGranoProperty.getValue();
			Double correccionRinde = Costos.getInstance().correccionCosechaProperty.getValue();
			
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Group group = new Group();//harvestMap
				ProcessHarvestMapTask umTask = new ProcessHarvestMapTask(group,
						store, precioGrano,correccionRinde);
				ProgressBar progressBarTask = new ProgressBar();			
				progressBarTask.setProgress(0);
				progressBarTask.progressProperty().bind(umTask.progressProperty());
				progressBox.getChildren().add(progressBarTask);
				Thread currentTaskThread = new Thread(umTask);
				currentTaskThread.setDaemon(true);
				currentTaskThread.start();

				umTask.setOnSucceeded(handler -> {
					harvestTree = (Quadtree) handler.getSource().getValue();//TODO en vez de pizarlo agregar las nuevas features
					harvestMap.getChildren().add(group);
					Bounds bl = harvestMap.getBoundsInLocal();
					System.out.println("bounds de harvestMap es: " + bl);
					//				harvestMap.setLayoutX(-bl.getMinX() + 75);
					//				harvestMap.setLayoutY(-bl.getMinY());
					// harvestMap.
					System.out.println("OpenHarvestMapTask succeded");



					progressBox.getChildren().remove(progressBarTask);


					//harvestMap.visibleProperty().set(true);
					//				System.out.println("termine de ocultar los poligonos");
					int size = this.harvestMap.getChildren().size();

//					Transition t = new Transition() {
//						int lastIndex=-1;
//						{
//							setCycleDuration(Duration.minutes(1));//modificar el tiempo para que siempre tarde lo mismo independientemente de cuantos nodos tenga
//							//setCycleDuration(60000);//tarda un minuto
//						}
//
//						/**
//						 * 
//						 * @param frac: es el porcentaje de avance de la animacion. va de 0 a 1
//						 */
//						@Override
//						protected void interpolate(double frac) {						
//							final int length = harvestMap.getChildren().size();
//							final double n =length * frac;//n va de  length a 0
//							//final int n = (int) (length - length *  frac);//n va de  length a 0
//							//	doTimelineScroll(lastIndex,n);
//
//							if(lastIndex == -1){
//								//	System.out.println("inicializando lastIndex con " +(length) );
//								lastIndex=0;
//							}
//							/**
//							 * recorro los poligonos de atras a adelante y los voy mostrando.
//							 */
//							//System.out.println("mostrando los poligonos desde "+lastIndex+" hasta = "+n+ " frac = "+frac);
//							for (int i = lastIndex; i < n ; i++) {							
//								harvestMap.getChildren().get(i).setVisible(true);
//							}
//							lastIndex = (int)(n);
//
//						}
//
//					};
					//t.play();

					timeline.setMax(size);
					timeline.setValue(size);
					timeline.setVisible(true);
					
					harvestMap.visibleProperty().set(true);
		
				});//fin del OnSucceeded
			}//fin del for stores
		
		}//if stores != null

	}
	
	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	private void doOpenGroupsMap() {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores();
		if (stores != null) {

			List<String> availableColumns = getAvailableColumns(stores.get(0));

			List<String> requieredColumns = new ArrayList<String>();
			requieredColumns.add("GroupBy");
			ColumnSelectDialog csd = new ColumnSelectDialog(
					requieredColumns, availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();
				CosechaItem.setColumnsMap(columns);
				
			} else {

				return;
			}
			harvestMap.getChildren().clear();
			resetMapScale();

		
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Group group = new Group();//harvestMap
				ProcessGroupsMapTask umTask = new ProcessGroupsMapTask(store,
						group, columns.getOrDefault("GroupBy", "elevacion"));
				ProgressBar progressBarTask = new ProgressBar();			
				progressBarTask.setProgress(0);
				progressBarTask.progressProperty().bind(umTask.progressProperty());
				progressBox.getChildren().add(progressBarTask);
				Thread currentTaskThread = new Thread(umTask);
				currentTaskThread.setDaemon(true);
				currentTaskThread.start();

				umTask.setOnSucceeded(handler -> {
					harvestTree = (Quadtree) handler.getSource().getValue();//TODO en vez de pizarlo agregar las nuevas features
					harvestMap.getChildren().add(group);
					Bounds bl = harvestMap.getBoundsInLocal();
					System.out.println("bounds de harvestMap es: " + bl);
			
					System.out.println("OpenHarvestMapTask succeded");

					progressBox.getChildren().remove(progressBarTask);

					int size = this.harvestMap.getChildren().size();

					harvestMap.visibleProperty().set(true);
		
				});//fin del OnSucceeded
			}//fin del for stores
		
		}//if stores != null

	}
	
	private void doCosecha3D() {
	
		if (this.harvestTree != null) {
			harvestMap.getChildren().clear();
			resetMapScale();
			
				Group group = new Group();//harvestMap
				
				ProcessHarvest3DMapTask umTask = new ProcessHarvest3DMapTask(group,
						harvestTree);
				
				ProgressBar progressBarTask = new ProgressBar();			
				progressBarTask.setProgress(0);
				progressBarTask.progressProperty().bind(umTask.progressProperty());
				progressBox.getChildren().add(progressBarTask);
				Thread currentTaskThread = new Thread(umTask);
				currentTaskThread.setDaemon(true);
				currentTaskThread.start();

				umTask.setOnSucceeded(handler -> {
				//	group = handler.getSource().getValue();//TODO en vez de pizarlo agregar las nuevas features
					harvestMap.getChildren().add(group);
					
					progressBox.getChildren().remove(progressBarTask);

					System.out.println("termine de crear la vista3d");
				});//fin de la transicion
			
			harvestMap.visibleProperty().set(true);
		}//if harvestTree != null
		
	}


	private void resetMapScale() {
		map.setScaleX(1);
		map.setScaleY(-1);
	}
	
//	/**
//	 * 
//	 * @param styles la clave es el color que tiene el path por origen y el value es el estilo a aplicarse
//	 */
//	private void doChangePathStyle(Group capa, Map<String,String> styles){
//		styles.keySet().forEach(key->{//recorro todas las claves
//			capa.getChildren().forEach(p->{//recorro todos los hijos de 
//				System.out.println(p.getStyleClass());
//				if(p.getStyleClass().contains(key)){
//					System.out.println("setting style white");
//					//p.setStyle("-fx-fill: white;");
//					p.setStyle(styles.get(key));
//				}
//			});
//		});
//
//	}

	private void doOpenFertMap() {

		FileDataStore store = chooseShapeFileAndGetStore();
		if (store != null) {
			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */
			List<String> availableColumns = getAvailableColumns(store);

			ColumnSelectDialog csd = new ColumnSelectDialog(
					FertilizacionItem.getRequiredColumns(), availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();

				FertilizacionItem.setColumnsMap(columns);
				System.out.println("columns map: " + columns);
			} else {
				System.out.println("columns names not set");
			}

			// // The Java 8 way to get the response value (with lambda
			// expression).
			// result.ifPresent(letter -> System.out.println("Your choice: " +
			// letter));

			/**/
			//map.getChildren().remove(fertMap);
			fertMap.getChildren().clear();
			resetMapScale();

			Double precioLabor =Costos.getInstance().precioLabFertProperty.getValue(); 
			Double precioInsumo = Costos.getInstance().precioFertProperty.getValue(); 

			Group fgroup = new Group();
			ProcessFertMapTask pfMapTask = new ProcessFertMapTask(
					fgroup, precioLabor, precioInsumo, store);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(
					pfMapTask.progressProperty());
			Thread currentTaskThread = new Thread(pfMapTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();

			pfMapTask.setOnSucceeded(handler -> {
				fertTree = (Quadtree) handler.getSource().getValue();
				fertMap.getChildren().add(fgroup);
				Bounds bl = fertMap.getBoundsInLocal();
				System.out.println("bounds de siembraMap es: " + bl);
				//				fertMap.setLayoutX(-bl.getMinX());
				//				fertMap.setLayoutY(-bl.getMinY());

				//		map.getChildren().add(fertMap);
				fertMap.visibleProperty().set(true);
				// Group taskMap = (Group) handler.getSource().getValue();
				System.out.println("OpenFertMapTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
		}
	}



	private void doOpenSiembraMap() {
		FileDataStore store = chooseShapeFileAndGetStore();
		if (store != null) {
			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */
			List<String> availableColumns = getAvailableColumns(store);

			ColumnSelectDialog csd = new ColumnSelectDialog(
					Siembra.getRequieredColumns(), availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();

				Siembra.setColumnsMap(columns);
				System.out.println("columns map: " + columns);
			} else {
				System.out.println("columns names not set");
			}

			//map.getChildren().remove(siembraMap);
			siembraMap.getChildren().clear();
			resetMapScale();
			Double precioLabor = Costos.getInstance().precioSiembraProperty.getValue(); 
			Double precioInsumo = Costos.getInstance().precioSemillaProperty.getValue();

			Group sgroup = new Group();
			ProcessSiembraMapTask psMapTask = new ProcessSiembraMapTask(
					sgroup, precioLabor,  precioInsumo, store);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(
					psMapTask.progressProperty());
			Thread currentTaskThread = new Thread(psMapTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();

			psMapTask.setOnSucceeded(handler -> {
				siembraTree = (Quadtree) handler.getSource().getValue();
				// set Layout x, y para que coincida con el origen de map
				siembraMap.getChildren().add(sgroup);
				
				Bounds bl = siembraMap.getBoundsInLocal();
				System.out.println("bounds de siembraMap es: " + bl);
				//					siembraMap.setLayoutX(-bl.getMinX());
				//					siembraMap.setLayoutY(-bl.getMinY());
				//	map.getChildren().add( siembraMap);
				siembraMap.visibleProperty().set(true);
				// Group taskMap = (Group) handler.getSource().getValue();
				System.out.println("OpenSiembraMapTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
		}
	}

	// leer mapa de pulverizaciones y calcular costos
	private void doOpenPulvMap() {
		FileDataStore store = chooseShapeFileAndGetStore();
		if (store != null) {
			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */
			List<String> availableColumns = getAvailableColumns(store);

			ColumnSelectDialog csd = new ColumnSelectDialog(
					PulverizacionItem.getRequieredColumns(), availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();

				PulverizacionItem.setColumnsMap(columns);
				System.out.println("columns map: " + columns);
			} else {
				System.out.println("columns names not set");
			}


			//	String precioPulv = precioPulvProperty.getValue();
			//System.out.println("precioPulv=" + precioPulv);

			//map.getChildren().remove(pulvMap);

			pulvMap.getChildren().clear();
			resetMapScale();
			Double precioLabor = Costos.getInstance().precioPulvProperty.getValue(); 

			Group pGroup = new Group();
			ProcessPulvMapTask pulvmTask = new ProcessPulvMapTask(pGroup,
					precioLabor, store);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(
					pulvmTask.progressProperty());
			Thread currentTaskThread = new Thread(pulvmTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();

			pulvmTask.setOnSucceeded(handler -> {
				pulvTree = (Quadtree) handler.getSource().getValue();
				pulvMap.getChildren().add(pGroup);
				Bounds bl = pulvMap.getBoundsInLocal();
				System.out.println("bounds de pulvMap es: " + bl);
				//				pulvMap.setLayoutX(-bl.getMinX());
				//				pulvMap.setLayoutY(-bl.getMinY());

				//	map.getChildren().add( pulvMap);
				pulvMap.visibleProperty().set(true);
				// Group taskMap = (Group) handler.getSource().getValue();
				System.out.println("OpenPulvMapTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
		}
	}

	private void doProcessMargin() {		
		FileDataStore store = chooseShapeFileAndGetStore();

		if (store != null) {

			System.out.println("processingMargins");
			//map.getChildren().remove(marginMap);
			marginMap.getChildren().clear();
			resetMapScale();
			Group mGroup = new Group();
			
			
			
			ProcessMarginMapTask uMmTask = new ProcessMarginMapTask(store, mGroup,
					pulvTree, fertTree, siembraTree, harvestTree);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(uMmTask.progressProperty());
			Thread currentTaskThread = new Thread(uMmTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();

			uMmTask.setOnSucceeded(handler -> {
				rentaTree = (Quadtree) handler.getSource().getValue();
				marginMap.getChildren().add(mGroup);
				
				Bounds bl = marginMap.getBoundsInLocal();
				System.out.println("bounds de marginMap es: " + bl);
				//			marginMap.setLayoutX(-bl.getMinX());
				//			marginMap.setLayoutY(-bl.getMinY());
				//	map.getChildren().add( marginMap);
				marginMap.visibleProperty().set(true);

				System.out.println("ProcessMarginTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
		}
	}

	private void doExportMargins() {
		@SuppressWarnings("unchecked")
		List<RentabilidadItem> rentas = this.rentaTree.queryAll();
		//	System.out.println("construyendo el shp para las rentas "+rentas.size());
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
				RentabilidadItem.getType());
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();

		for (RentabilidadItem renta : rentas) {
			SimpleFeature rentaFeature = renta.getFeature(featureBuilder);
			features.add(rentaFeature);
			//	System.out.println("agregando a features "+rentaFeature);

		}

		File shapeFile =  getNewShapeFile();

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		try {
			params.put("url", shapeFile.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		params.put("create spatial index", Boolean.TRUE);


		ShapefileDataStore newDataStore=null;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(RentabilidadItem.getType());

			//		System.out.println("antes de forzar wgs 84");

			/*
			 * You can comment out this line if you are using the createFeatureType
			 * method (at end of class file) rather than DataUtilities.createType
			 */
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String typeName = newDataStore.getTypeNames()[0];
		//	System.out.println("typeName 0 del newDataStore es "+typeName);
		SimpleFeatureSource featureSource = null;
		try {
			featureSource = newDataStore.getFeatureSource(typeName);
			//	System.out.println("cree new featureSource "+featureSource.getInfo());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (featureSource instanceof SimpleFeatureStore) {			

			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			Transaction transaction = new DefaultTransaction("create");
			featureStore.setTransaction(transaction);

			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */
			SimpleFeatureCollection collection = new ListFeatureCollection(RentabilidadItem.getType(), features);
			//	System.out.println("agregando features al store " +collection.size());
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
				//	System.out.println("commiting transaction "+ featureStore.getCount(new Query()));
			} catch (Exception problem) {
				problem.printStackTrace();
				try {
					transaction.rollback();
					//	System.out.println("transaction rolledback");
				} catch (IOException e) {

					e.printStackTrace();
				}

			} finally {
				try {
					transaction.close();
					//System.out.println("closing transaction");
				} catch (IOException e) {

					e.printStackTrace();
				}
			}

		}		
	}

	private void doExportHarvest() {
		@SuppressWarnings("unchecked")
		List<CosechaItem> cosechas = this.harvestTree.queryAll();
		//	System.out.println("construyendo el shp para las rentas "+rentas.size());
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
				CosechaItem.getType());
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();

		//cosechas.sort((a,b)->a.compareTo(b));
		for (CosechaItem cosecha : cosechas) {
			SimpleFeature cosechaFeature = cosecha.getFeature(featureBuilder);
			features.add(cosechaFeature);
			//	System.out.println("agregando a features "+rentaFeature);

		}

		File shapeFile =  getNewShapeFile();

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		try {
			params.put("url", shapeFile.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		params.put("create spatial index", Boolean.TRUE);


		ShapefileDataStore newDataStore=null;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(CosechaItem.getType());

			//		System.out.println("antes de forzar wgs 84");

			/*
			 * You can comment out this line if you are using the createFeatureType
			 * method (at end of class file) rather than DataUtilities.createType
			 */
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String typeName = newDataStore.getTypeNames()[0];
		//	System.out.println("typeName 0 del newDataStore es "+typeName);
		SimpleFeatureSource featureSource = null;
		try {
			featureSource = newDataStore.getFeatureSource(typeName);
			//	System.out.println("cree new featureSource "+featureSource.getInfo());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (featureSource instanceof SimpleFeatureStore) {			

			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			Transaction transaction = new DefaultTransaction("create");
			featureStore.setTransaction(transaction);

			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */
			SimpleFeatureCollection collection = new ListFeatureCollection(CosechaItem.getType(), features);
			//	System.out.println("agregando features al store " +collection.size());
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
				//	System.out.println("commiting transaction "+ featureStore.getCount(new Query()));
			} catch (Exception problem) {
				problem.printStackTrace();
				try {
					transaction.rollback();
					//	System.out.println("transaction rolledback");
				} catch (IOException e) {

					e.printStackTrace();
				}

			} finally {
				try {
					transaction.close();
					//System.out.println("closing transaction");
				} catch (IOException e) {

					e.printStackTrace();
				}
			}

		}		
	}

	private void doSnapshot(){

		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT);

		WritableImage image = map.snapshot(params, null);



		// WritableImage image = barChart.snapshot(new SnapshotParameters(), null);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Image");
		
		
		File lastFile = null;
		String lastFileName =  Configuracion.getInstance().getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile != null ){
			fileChooser.setInitialDirectory(lastFile.getParentFile());
			fileChooser.setInitialFileName(lastFile.getName());
		}
		
	//	if(file!=null)		fileChooser.setInitialDirectory(file.getParentFile());
		// Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
				"PNG files (*.png)", "*.png");
		fileChooser.getExtensionFilters().add(extFilter);
		// Show save file dialog
		File snapsthotFile = fileChooser.showSaveDialog(this.stage);

		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", snapsthotFile);
		} catch (IOException e) {
		e.printStackTrace();
		}
	}

	private List<String> getAvailableColumns(FileDataStore store) {
		List<String> availableColumns = new ArrayList<String>();

		SimpleFeatureType sch;
		try {
			sch = store.getSchema();
			List<AttributeType> types = sch.getTypes();
			for (AttributeType at : types) {
				availableColumns.add(at.getName().toString());
			}

		} catch (IOException e) {			
			e.printStackTrace();
		}
		return availableColumns;
	}

	private FileDataStore chooseShapeFileAndGetStore() {
		FileDataStore store = null;
		try{
			store = chooseShapeFileAndGetMultipleStores().get(0);
		}catch(Exception e ){
			e.printStackTrace();
		}
		return store;
	}

	private List<FileDataStore> chooseShapeFileAndGetMultipleStores() {
		List<File> files =chooseFiles("SHP", "*.shp");;
		List<FileDataStore> stores = new ArrayList<FileDataStore>();
		if (files != null) {
			for(File f : files){
				try {
					stores.add(FileDataStoreFinder.getDataStore(f));
				} catch (IOException e) {
					e.printStackTrace();
				}
				stage.setTitle(TITLE_VERSION+" "+f.getName());
				Configuracion.getInstance().setProperty(Configuracion.LAST_FILE,f.getAbsolutePath());

			}


			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */

		}
		return stores;
	}
	
	/**
	 * 
	 * @param f1 filter Title "JPG"
	 * @param f2 filter regex "*.jpg"
	 */
		private List<File> chooseFiles(String f1,String f2) {
			List<File> files =null;
			FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(f1, f2));
			File lastFile = null;
			String lastFileName =  Configuracion.getInstance().getPropertyOrDefault(Configuracion.LAST_FILE,null);
			if(lastFileName != null){
				lastFile = new File(lastFileName);
			}
			if(lastFile != null ){
				fileChooser.setInitialDirectory(lastFile.getParentFile());
				fileChooser.setInitialFileName(lastFile.getName());
			}
			try{
				files = fileChooser.showOpenMultipleDialog(new Stage());
		//		file = files.get(0);
			}catch(IllegalArgumentException e){
				fileChooser.setInitialDirectory(null);
				File file = fileChooser.showOpenDialog(new Stage());
				files = new LinkedList<File>();
				files.add(file);
			}
			
			if(files!=null && files.size()>0){
				File f = files.get(0);
				Configuracion.getInstance().setProperty(Configuracion.LAST_FILE,f.getAbsolutePath());		
			}
			
			return files;
		}


	private  File getNewShapeFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Guardar ShapeFile");
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("SHP", "*.shp"));
		
		File lastFile = null;
		String lastFileName =  Configuracion.getInstance().getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile != null ){
			fileChooser.setInitialDirectory(lastFile.getParentFile());
			fileChooser.setInitialFileName(lastFile.getName());
		}
		
		//if(file!=null)	fileChooser.setInitialDirectory(file.getParentFile());

		File file = fileChooser.showSaveDialog(new Stage());

		System.out.println("archivo seleccionado para guardar "+file);

		return file;
	}


	/**
	 * Metodo que oculta todos los paths que estan despues del fueature
	 * seleccionado y muestra todos los que estan antes
	 * 
	 * @param ScrollEvent
	 *            e
	 */
	private void doTimelineScroll(Number old_val, Number new_val) {

		int size = this.harvestMap.getChildren().size() - 1;
		timeline.setMax(size);
		int init = 0;
		int fin = 0;

		// Si el valor anterior es menor que el actual desoculto, de lo
		// contrario oculto
		Boolean visible = old_val.intValue() < new_val.intValue();
		if (visible) {
			init = old_val.intValue();
			fin = new_val.intValue();
		} else {
			init = new_val.intValue();
			fin = old_val.intValue();
		}
		//TODO poner esto en una transicion a ver si se hace mas agil
		for (int i = init; i < fin; i++) {// Node n : map1.getChildren()){
			// System.out.println("making visible ="+visible+" at node "+(size-i));
			int indice = size - i;
			if (indice >0 && indice < size) {// con esto controlo que no haya out of bounds
				Node n = harvestMap.getChildren().get(indice);
				//n.setVisible(visible);
				if(visible){
					n.setOpacity(1);
				}else{
					n.setOpacity(0.1);
				}

			}
		}
	}

	

	// private Path addPathToMapWithColor(Color color, Geometry polygon) {
	// Coordinate[] coords = polygon.getCoordinates();
	// Path path = new Path();
	// path.setStrokeWidth(0.05);
	//
	// path.setFill(color);// (colors[currentColor]);
	// path.getElements().add(new MoveTo(coords[0].x, coords[0].y));
	// for (int i = 1; i < coords.length; i++) {
	// path.getElements().add(new LineTo(coords[i].x, coords[i].y));
	// }
	// path.getElements().add(new LineTo(coords[0].x, coords[0].y));
	// map1.getChildren().add(path);
	// return path;
	// }

	// float calcPoligonArea(Coordinate[] coords) {
	//
	// // float x[N],y[N];
	// float sum_but_no_result = 0;
	// int N = coords.length;
	// for (int i = 0; i < (coords.length - 1); i++) // N is point number of
	// // polygon
	// {
	// sum_but_no_result += coords[i].x * coords[i + 1].y + coords[i].y
	// * coords[i + 1].x;
	// }
	// sum_but_no_result += coords[N - 1].x * coords[0].y + coords[N - 1].y
	// * coords[0].x;
	//
	// float sum = (float) Math.abs(sum_but_no_result) / 2.0f;
	// return sum;
	// }
	//
	//
	//
	// private void saveFeaturesToNewShp(SimpleFeatureCollection features) {
	// File destinationShpFile = createDestinationShapeFile();// new
	// // File("C:\\Users\\tomas\\Documents\\newShapeFile.shp"));
	// if (destinationShpFile == null)
	// return;
	// Transaction transaction = new DefaultTransaction("create");
	// try {
	// Map<String, Serializable> params = new HashMap<String, Serializable>();
	// params.put("url", destinationShpFile.toURI().toURL());
	// params.put("create spatial index", Boolean.TRUE);
	//
	// ShapefileDataStoreFactory dataStoreFactory = new
	// ShapefileDataStoreFactory();
	// ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory
	// .createNewDataStore(params);
	//
	// newDataStore.createSchema(features.getSchema());
	// newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
	//
	// ((SimpleFeatureStore) newDataStore).setTransaction(transaction);
	//
	// try {
	// ((SimpleFeatureStore) newDataStore).addFeatures(features);
	// transaction.commit();
	//
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// transaction.rollback();
	//
	// } finally {
	// transaction.close();
	// }
	// } catch (Exception e) {
	// System.out.println("Ups something went wrong!!");
	// } finally {
	// try {
	// transaction.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	//
	// /**
	// * Prompt the user for the name and path to use for the output shapefile
	// *
	// * @param csvFile
	// * the input csv file used to create a default shapefile name
	// *
	// * @return name and path for the shapefile as a new File object
	// */
	// private File createDestinationShapeFile() {
	// String path = "C:\\Users\\tomas\\Documents\\newShapeFile.shp";//
	// oldFile.getAbsolutePath();
	// String newPath = path.substring(0, path.length() - 4) + ".shp";
	//
	// JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
	// chooser.setDialogTitle("Save shapefile");
	// chooser.setSelectedFile(new File(newPath));
	//
	// int returnVal = chooser.showSaveDialog(null);
	//
	// if (returnVal != JFileDataStoreChooser.APPROVE_OPTION) {
	// // the user cancelled the dialog
	// // System.exit(0);
	// return null;
	// }
	//
	// File newFile = chooser.getSelectedFile();
	// // if (newFile.equals(oldFile)) {
	// // System.out.println("Error: cannot replace " + oldFile);
	// // System.exit(0);
	// // }
	//
	// return newFile;
	// }
	//
	// private Path drawSemiRing(double centerX, double centerY, double radius,
	// double innerRadius, Color bgColor, Color strkColor) {
	// Path path = new Path();
	// path.setFill(bgColor);
	// path.setStroke(strkColor);
	// path.setFillRule(FillRule.EVEN_ODD);
	//
	// MoveTo moveTo = new MoveTo();
	// moveTo.setX(centerX + innerRadius);
	// moveTo.setY(centerY);
	//
	// ArcTo arcToInner = new ArcTo();
	// arcToInner.setX(centerX - innerRadius);
	// arcToInner.setY(centerY);
	// arcToInner.setRadiusX(innerRadius);
	// arcToInner.setRadiusY(innerRadius);
	//
	// MoveTo moveTo2 = new MoveTo();
	// moveTo2.setX(centerX + innerRadius);
	// moveTo2.setY(centerY);
	//
	// HLineTo hLineToRightLeg = new HLineTo();
	// hLineToRightLeg.setX(centerX + radius);
	//
	// ArcTo arcTo = new ArcTo();
	// arcTo.setX(centerX - radius);
	// arcTo.setY(centerY);
	// arcTo.setRadiusX(radius);
	// arcTo.setRadiusY(radius);
	//
	// HLineTo hLineToLeftLeg = new HLineTo();
	// hLineToLeftLeg.setX(centerX - innerRadius);
	//
	// path.getElements().add(moveTo);
	// path.getElements().add(arcToInner);
	// path.getElements().add(moveTo2);
	// path.getElements().add(hLineToRightLeg);
	// path.getElements().add(arcTo);
	// path.getElements().add(hLineToLeftLeg);
	//
	// return path;
	// }


	private Parent createZoomPane( Group group) {
		final double SCALE_DELTA = 1.1;
		final StackPane zoomPane = new StackPane();

		zoomPane.getChildren().add(group);

		final ScrollPane scroller = new ScrollPane();
		final Group scrollContent = new Group(zoomPane);
		scroller.setContent(scrollContent);

		//		    scroller.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
		//		      @Override
		//		      public void changed(ObservableValue<? extends Bounds> observable,
		//		          Bounds oldValue, Bounds newValue) {
		//		        zoomPane.setMinSize(newValue.getWidth(), newValue.getHeight());
		//		      }
		//		    });
		//
		//		    scroller.setPrefViewportWidth(1500);
		//		    scroller.setPrefViewportHeight(800);
		//scroller.setViewportBounds( new BoundingBox(100,100));
		
		
//		scroller.setPrefViewportWidth(scene.getWidth()-15);						
		scroller.setPrefViewportHeight(scene.getHeight()*DIVIDER_POSITION);		
		horizontalSplit.setPrefWidth(scene.getWidth());
		horizontalSplit.setPrefHeight(scene.getHeight());

		scene.widthProperty().addListener(new ChangeListener<Number>(){

			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number arg2) {
				scroller.setPrefViewportWidth(arg2.doubleValue()*DIVIDER_POSITION);		
				horizontalSplit.setPrefWidth(arg2.doubleValue());
				//System.out.println("scene Width Property changed "+arg2);
			}				
		});

		scene.heightProperty().addListener(new ChangeListener<Number>(){

			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number arg2) {
					scroller.setPrefViewportHeight(arg2.doubleValue());		
				horizontalSplit.setPrefHeight(arg2.doubleValue());
			//	System.out.println("scene Heigth Property changed "+arg2);
			}				
		});

		
		final ObjectProperty<Point2D> lastMouseCoordinates = new SimpleObjectProperty<Point2D>();
		
		zoomPane.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				event.consume();

				if (event.getDeltaY() == 0) {
					return;
				}

				
				
	
				double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA
						: 1 / SCALE_DELTA;

				// amount of scrolling in each direction in scrollContent coordinate
				// units
				Point2D scrollOffset = figureScrollOffset(scrollContent, scroller);
				scrollOffset = scrollOffset.add(lastMouseCoordinates.get());//
				
				
				group.setScaleX(group.getScaleX() * scaleFactor);
				group.setScaleY(group.getScaleY() * scaleFactor);

				// move viewport so that old center remains in the center after the
				// scaling
				repositionScroller(scrollContent, scroller, scaleFactor, scrollOffset);

			}
		});

		// Panning via drag....
	
		
		scrollContent.setOnMousePressed(event ->  {
				lastMouseCoordinates.set(new Point2D(event.getX(), event.getY()));
			});
		
		scrollContent.setOnMouseMoved(event ->  {
			lastMouseCoordinates.set(new Point2D(event.getX(), event.getY()));
			//System.out.println("mouse moved "+lastMouseCoordinates.get() );
		});

		scrollContent.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				double deltaX = event.getX() - lastMouseCoordinates.get().getX();
				double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
				double deltaH = deltaX * (scroller.getHmax() - scroller.getHmin()) / extraWidth;
				double desiredH = scroller.getHvalue() - deltaH;
				scroller.setHvalue(Math.max(0, Math.min(scroller.getHmax(), desiredH)));// esto evita que haga scroll fuera del tama�o max del scroller

				double deltaY = event.getY() - lastMouseCoordinates.get().getY();
				double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
				double deltaV = deltaY * (scroller.getHmax() - scroller.getHmin()) / extraHeight;
				double desiredV = scroller.getVvalue() - deltaV;
				scroller.setVvalue(Math.max(0, Math.min(scroller.getVmax(), desiredV)));
			}
		});

		return scroller;
	}//fin del zoompane

	private Point2D figureScrollOffset(Node scrollContent, ScrollPane scroller) {
		double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
		double hScrollProportion = (scroller.getHvalue() - scroller.getHmin()) / (scroller.getHmax() - scroller.getHmin());
		double scrollXOffset = hScrollProportion * Math.max(0, extraWidth);
		double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
		double vScrollProportion = (scroller.getVvalue() - scroller.getVmin()) / (scroller.getVmax() - scroller.getVmin());
		double scrollYOffset = vScrollProportion * Math.max(0, extraHeight);
		return new Point2D(scrollXOffset, scrollYOffset);
	}

	private void repositionScroller(Node scrollContent, ScrollPane scroller, double scaleFactor, Point2D scrollOffset) {
		double scrollXOffset = scrollOffset.getX();
		double scrollYOffset = scrollOffset.getY();
		double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
		if (extraWidth > 0) {
			double halfWidth = scroller.getViewportBounds().getWidth() / 2 ;
			double newScrollXOffset = (scaleFactor - 1) *  halfWidth + scaleFactor * scrollXOffset;
			scroller.setHvalue(scroller.getHmin() + newScrollXOffset * (scroller.getHmax() - scroller.getHmin()) / extraWidth);
		} else {
			scroller.setHvalue(scroller.getHmin());
		}
		double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
		if (extraHeight > 0) {
			double halfHeight = scroller.getViewportBounds().getHeight() / 2 ;
			double newScrollYOffset = (scaleFactor - 1) * halfHeight + scaleFactor * scrollYOffset;
			scroller.setVvalue(scroller.getVmin() + newScrollYOffset * (scroller.getVmax() - scroller.getVmin()) / extraHeight);
		} else {
			scroller.setHvalue(scroller.getHmin());
		}
	}
}