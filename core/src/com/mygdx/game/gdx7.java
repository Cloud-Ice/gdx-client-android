package com.mygdx.game;

import java.net.URISyntaxException;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import org.json.*;
import io.socket.emitter.Emitter;
import io.socket.client.IO;
import io.socket.client.Socket;

public class gdx7 extends InputAdapter implements ApplicationListener {
	SpriteBatch batch;
	Texture img;
	public Socket socket;
    protected Stage stage;

	public static class GameObject extends ModelInstance {
		public final Vector3 center = new Vector3();
		public final Vector3 dimensions = new Vector3();
		public final float radius;
		public String name;

		private final static BoundingBox bounds = new BoundingBox();

		public GameObject(Model model, String rootNode, boolean mergeTransform) {
			super(model, rootNode, mergeTransform);
			calculateBoundingBox(bounds);
			bounds.getCenter(center);
			bounds.getDimensions(dimensions);
			radius = dimensions.len() / 2f;
		}
		public GameObject(Model model) {
			super(model);
			calculateBoundingBox(bounds);
			bounds.getCenter(center);
			bounds.getDimensions(dimensions);
			radius = dimensions.len() / 2f;
		}
	}

	private int selected = -1, selecting = -1;
	private Material selectionMaterial;
	private Material originalMaterial;
	public Environment environment;
	public PerspectiveCamera cam;
	public CameraInputController camController;
	public ModelBatch modelBatch;
	public Model model;
	public ModelInstance instance;
	public AssetManager assets;
	public boolean loading;

	private BitmapFont font;
	private SpriteBatch spriteBatch;

	private Vector3 position = new Vector3();
	private Vector3 positionAppear = new Vector3();

	public Array<GameObject> instances = new Array<GameObject>();

	public AnimationController controller;
	public String nameUser = "Jose";

	/*buttons*/
    private TextureAtlas buttonsAtlas; 
    private Skin buttonSkin; 
    private TextButton button; 
    private TextButton buttonConnect; 
	
    private boolean mostrarTeclado = false; 
    
	@Override
	public void create() {
        stage = new Stage();
		try {
			socket = IO.socket("http://ioserver-antrax.rhcloud.com:8000");
			//socket = IO.socket("http://192.168.0.92:8000");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				socket.emit("join", nameUser );
			}
		}).on("message", new Emitter.Listener() {
			@Override
			public void call(final Object... args) {
				System.out.println("recibido");
			}
		}).on("join user", new Emitter.Listener() {
			@Override
			public void call(final Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String nameplayer = data.getString("name");
					int idplayer = -1;
					for( int i = 0; i< instances.size ; i++){
						GameObject instance = instances.get(i);
						if (instance.name.equals(nameplayer)){
							idplayer = i;
							continue;
						}
					}
					if (idplayer == -1) crearObjeto(nameplayer);
				} catch (JSONException e) {
					return;
				}
			}
		}).on("action", new Emitter.Listener() {
			@Override
			public void call(final Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String nameplayer = data.getString("name");
					System.out.println("nameplayer:" + nameplayer);
					Float x = Float.parseFloat(data.getString("x"));
					Float y = Float.parseFloat(data.getString("y"));
					Float z = Float.parseFloat(data.getString("z"));
					int idplayer = 0;
					for( int i = 0; i< instances.size ; i++){
						GameObject instance = instances.get(i);
						System.out.println("inst : " + instance.name );
						if (instance.name.equals(nameplayer)){
							idplayer = i;
							continue;
						}
					}
					System.out.println("nameplayer:" + nameplayer + " x:" + x + " y:" + y + " z:" + z);
					if (idplayer > 0) moverObjeto(idplayer, x, y, z);
				} catch (JSONException e) {
					System.out.println("json e: " + e.getMessage());
					return;
				}

			}
		}).on("list user", new Emitter.Listener() {
			@Override
			public void call(final Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String nameplayer = data.getString("name");
					System.out.println("nameplayer:" + nameplayer);
					Float x = Float.parseFloat(data.getString("x"));
					Float y = Float.parseFloat(data.getString("y"));
					Float z = Float.parseFloat(data.getString("z"));
					int idplayer = -1;
					for( int i = 0; i< instances.size ; i++){
						GameObject instance = instances.get(i);
						if (instance.name.equals(nameplayer)){
							idplayer = i;
							continue;
						}
					}
					if (idplayer == -1) crearObjeto(nameplayer, x, y, z);
				} catch (JSONException e) {
					System.out.println("json e: " + e.getMessage());
					return;
				}

			}
		}).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... args) {
			}
		});
		
		
		spriteBatch = new SpriteBatch();
		font = new BitmapFont(Gdx.files.internal("data/arial-32-pad.fnt"), false);
		font.getData().markupEnabled = true;
		font.getData().breakChars = new char[] { '-' };
/*start create button*/
		buttonsAtlas = new TextureAtlas("imageButtons/buttons.pack"); //** button atlas image **// 
	    buttonSkin = new Skin();
	    buttonSkin.addRegions(buttonsAtlas); //** skins for on and off **//
	    TextButtonStyle style = new TextButtonStyle(); //** Button properties **//
        style.up = buttonSkin.getDrawable("buttonOff");
        style.down = buttonSkin.getDrawable("buttonOn");
        style.font = font;
        button = new TextButton("Cambiar Login", style); //** Button text and style **//
        button.setPosition(0, 100); //** Button location **//
        button.setHeight(50); //** Button Height **//
        button.setWidth(250); //** Button Width **//
        button.addListener(new InputListener() {
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
            		System.out.println("pressed");
                    //Gdx.input.setOnscreenKeyboardVisible(true);
                    //mostrarTeclado = true;
                    return true;
            }
            
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                    System.out.println("touch up ");
                    Gdx.input.setOnscreenKeyboardVisible(true); 
            }
        });
        buttonConnect = new TextButton("Conectar", style); //** Button text and style **//
        buttonConnect.setPosition(0, 200); //** Button location **//
        buttonConnect.setHeight(50); //** Button Height **//
        buttonConnect.setWidth(250); //** Button Width **//
        buttonConnect.addListener(new InputListener() {
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
            		System.out.println("pressed");
                    return true;
            }
            
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                    System.out.println("touch up ");
            		socket.connect();
            }
        });
        stage.addActor(button);
        stage.addActor(buttonConnect);
/*end create button*/
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		modelBatch = new ModelBatch();

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0f, 14f, -10f);
		cam.lookAt(0, 0, 0);
		cam.near = 1f;
		cam.far = 300f;
		cam.update();

		// ModelLoader loader = new ObjLoader();

		// model = loader.loadModel(Gdx.files.internal("goals.g3db"));
		// assets.load("ship.g3db", Model.class);
		// model = assets.get("ship.g3db", Model.class);

		/*
		 * ModelBuilder modelBuilder = new ModelBuilder(); model =
		 * modelBuilder.createBox(5f, 5f, 5f, new
		 * Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position |
		 * Usage.Normal);
		 */
		// instance = new ModelInstance(model);
		assets = new AssetManager();
		assets.load("ch.g3dj", Model.class);

		// assets.load("ship.g3db", Model.class);
		// model = assets.get("ship.g3db", Model.class);

		selectionMaterial = new Material();
		selectionMaterial.set(ColorAttribute.createDiffuse(Color.ORANGE));
		originalMaterial = new Material();

		camController = new CameraInputController(cam);
		Gdx.input.setInputProcessor(new InputMultiplexer(this, camController));
		//Gdx.input.setInputProcessor(camController);
		loading = true;
        Gdx.input.setInputProcessor(new InputMultiplexer(this, stage)); 
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		selecting = getObject(screenX, screenY);
		return selecting >= 0;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		//return selecting >= 0;
		if (selecting < 0)
			return false;
		if (selected == selecting) {
			Ray ray = cam.getPickRay(screenX, screenY);
			final float distance = -ray.origin.y / ray.direction.y;
			position.set(ray.direction).scl(distance).add(ray.origin);
			instances.get(selected).transform.setTranslation(position);


			socket.emit("action",nameUser+";"+position.x+";"+position.y+";"+position.z);
		}
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (selecting >= 0) {
			if (selecting == getObject(screenX, screenY))
				setSelected(selecting);
			selecting = -1;
			return true;
		}
		return false;
	}

	public void setSelected(int value) {
		if (selected == value)
			return;
		if (selected >= 0) {
			Material mat = instances.get(selected).materials.get(0);
			mat.clear();
			mat.set(originalMaterial);
		}
		selected = value;
		if (selected >= 0) {
			Material mat = instances.get(selected).materials.get(0);
			originalMaterial.clear();
			originalMaterial.set(mat);
			mat.clear();
			mat.set(selectionMaterial);
		}
	}

	public int getObject(int screenX, int screenY) {
		Ray ray = cam.getPickRay(screenX, screenY);

		int result = -1;
		float distance = -1;

		for (int i = 0; i < instances.size; ++i) {
			final GameObject instance = instances.get(i);

			instance.transform.getTranslation(position);
			position.add(instance.center);
			System.out.println("instances" + i );

			final float len = ray.direction.dot(position.x - ray.origin.x, position.y - ray.origin.y,
					position.z - ray.origin.z);
			if (len < 0f)
				continue;

			float dist2 = position.dst2(ray.origin.x + ray.direction.x * len, ray.origin.y + ray.direction.y * len,
					ray.origin.z + ray.direction.z * len);
			if (distance >= 0f && dist2 > distance)
				continue;

			if (dist2 <= instance.radius * instance.radius) {
				result = i;
				distance = dist2;
			}
		}
		return result;
	}

	private void crearObjeto(String nombre){
		GameObject instance = new GameObject(model);

		//positionAppear.add(0,1,1);
		instance.transform.setTranslation(1f,0f,0f);
		instance.name = nombre;
		instances.add(instance);
	}

	private void crearObjeto(String nombre,float x, float y, float z){
		GameObject instance = new GameObject(model);

		//positionAppear.add(0,1,1);
		instance.transform.setTranslation(x,y,z);
		instance.name = nombre;
		instances.add(instance);
	}

	private void moverObjeto(int idplayer, float x, float y, float z){
		GameObject instance = instances.get(idplayer);

		//positionAppear.add(0,1,1);
		instance.transform.setTranslation(x, y, z);
		//instance.transform.translate(x,y,z);

	}

	private void doneLoading() {
		// assets.load("ship.g3db", Model.class);
		// model = assets.get("panel.g3dj", Model.class);
		model = assets.get("ch.g3dj", Model.class);

		String id = model.nodes.get(0).id;
		GameObject instance = new GameObject(model);

		instance.name = nameUser;
		// instance = new ModelInstance(model);
		controller = new AnimationController(instance);
		// controller.setAnimation("Idle");
		controller.animate("Armature|Walk", -1, 1f, null, 0.2f);
	

		// controller.animate("Walk", -1, 1f, null, 0.2f);

		// controller.animate("Idle", -1, 1f, null, 0.2f);
		// controller.animate("walk", 0);
		// System.out.println("anim : " + controller.);
		// Model ship = assets.get("data/ship.obj", Model.class);
		// ModelInstance shipInstance = new ModelInstance(ship);
		instances.add(instance);
		loading = false;
	}

	@Override
	public void render() {
		if (loading && assets.update())
			doneLoading();
		camController.update();

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		// controller.update(Gdx.graphics.getDeltaTime());

		spriteBatch.begin();
		font.draw(spriteBatch, "Usuario:" + nameUser, 000, 300, 400, Align.left, true);
		spriteBatch.renderCalls = 0;
		spriteBatch.end();
		
		//if (Gdx.input.justTouched())
		if (mostrarTeclado)		Gdx.input.setOnscreenKeyboardVisible(true);
		
		modelBatch.begin(cam);

		for (final GameObject instance : instances) {
			//if (isVisible(cam, instance)) {
			modelBatch.render(instance, environment);
			//visibleCount++;
			//}
		}

		// modelBatch.render(instances, environment);
		// modelBatch.render(instance, environment);
		modelBatch.end();
		
		stage.draw();
		if (model != null)
			controller.update(Gdx.graphics.getDeltaTime());
	}
	
	@Override
	public boolean keyTyped (char character) {
		if (character == '\b' && nameUser.length() >= 1) {
			nameUser = nameUser.substring(0, nameUser.length() - 1);
		} else if (character == '\n') {
			Gdx.input.setOnscreenKeyboardVisible(false);
		} else {
			nameUser += character;
		}
		return false;
	}
	
	protected boolean isVisible(final Camera cam, final GameObject instance) {
		instance.transform.getTranslation(position);
		position.add(instance.center);
		return cam.frustum.sphereInFrustum(position, instance.radius);
	}

	@Override
	public void dispose() {
		modelBatch.dispose();
		instances.clear();
		assets.dispose();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
