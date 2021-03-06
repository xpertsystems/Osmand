package net.osmand.plus.mapcontextmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.mapcontextmenu.MenuController.MenuState;
import net.osmand.plus.mapcontextmenu.MenuController.TitleButtonController;
import net.osmand.plus.mapcontextmenu.MenuController.TitleProgressController;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.HorizontalSwipeConfirm;
import net.osmand.plus.views.controls.SingleTapConfirm;
import net.osmand.util.Algorithms;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static net.osmand.plus.mapcontextmenu.MenuBuilder.SHADOW_HEIGHT_TOP_DP;


public class MapContextMenuFragment extends BaseOsmAndFragment implements DownloadEvents {
	public static final String TAG = "MapContextMenuFragment";

	public static final float FAB_PADDING_TOP_DP = 4f;
	public static final float MARKER_PADDING_DP = 20f;
	public static final float MARKER_PADDING_X_DP = 50f;
	public static final float SKIP_HALF_SCREEN_STATE_KOEF = .21f;
	public static final int ZOOM_IN_STANDARD = 17;

	private View view;
	private View mainView;
	private ImageView fabView;
	private View zoomButtonsView;
	private ImageButton zoomInButtonView;
	private ImageButton zoomOutButtonView;

	private MapContextMenu menu;
	private OnLayoutChangeListener containerLayoutListener;

	private int menuTopViewHeight;
	private int menuTopShadowHeight;
	private int menuTopShadowAllHeight;
	private int menuTitleHeight;
	private int menuBottomViewHeight;
	private int menuFullHeight;
	private int menuFullHeightMax;
	private int menuTopViewHeightExcludingTitle;
	private int menuTitleTopBottomPadding;

	private int screenHeight;
	private int viewHeight;
	private int zoomButtonsHeight;

	private int fabPaddingTopPx;
	private int markerPaddingPx;
	private int markerPaddingXPx;

	private OsmandMapTileView map;
	private LatLon mapCenter;
	private int mapZoom;
	private int origMarkerX;
	private int origMarkerY;
	private boolean customMapCenter;
	private boolean moving;
	private boolean nightMode;
	private boolean centered;
	private boolean initLayout = true;
	private boolean wasDrawerDisabled;
	private boolean zoomIn;

	private float skipHalfScreenStateLimit;

	private int screenOrientation;
	private boolean created;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		processScreenHeight(container);

		fabPaddingTopPx = dpToPx(FAB_PADDING_TOP_DP);
		markerPaddingPx = dpToPx(MARKER_PADDING_DP);
		markerPaddingXPx = dpToPx(MARKER_PADDING_X_DP);

		menu = getMapActivity().getContextMenu();
		view = inflater.inflate(R.layout.map_context_menu_fragment, container, false);
		if (!menu.isActive()) {
			return view;
		}
		nightMode = menu.isNightMode();
		mainView = view.findViewById(R.id.context_menu_main);

		map = getMapActivity().getMapView();
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		customMapCenter = menu.getMapCenter() != null;
		if (!customMapCenter) {
			mapCenter = box.getCenterLatLon();
			menu.setMapCenter(mapCenter);
			double markerLat = menu.getLatLon().getLatitude();
			double markerLon = menu.getLatLon().getLongitude();
			origMarkerX = (int) box.getPixXFromLatLon(markerLat, markerLon);
			origMarkerY = (int) box.getPixYFromLatLon(markerLat, markerLon);
		} else {
			mapCenter = menu.getMapCenter();
			origMarkerX = box.getCenterPixelX();
			origMarkerY = box.getCenterPixelY();
		}
		mapZoom = menu.getMapZoom();
		if (mapZoom == 0) {
			mapZoom = map.getZoom();
		}

		// Left title button
		final Button leftTitleButton = (Button) view.findViewById(R.id.title_button);
		leftTitleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController leftTitleButtonController = menu.getLeftTitleButtonController();
				if (leftTitleButtonController != null) {
					leftTitleButtonController.buttonPressed();
				}
			}
		});

		// Right title button
		final Button rightTitleButton = (Button) view.findViewById(R.id.title_button_right);
		rightTitleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController rightTitleButtonController = menu.getRightTitleButtonController();
				if (rightTitleButtonController != null) {
					rightTitleButtonController.buttonPressed();
				}
			}
		});

		// Left subtitle button
		final Button leftSubtitleButton = (Button) view.findViewById(R.id.subtitle_button);
		leftSubtitleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController leftSubtitleButtonController = menu.getLeftSubtitleButtonController();
				if (leftSubtitleButtonController != null) {
					leftSubtitleButtonController.buttonPressed();
				}
			}
		});

		// Left download button
		final Button leftDownloadButton = (Button) view.findViewById(R.id.download_button_left);
		leftDownloadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController leftDownloadButtonController = menu.getLeftDownloadButtonController();
				if (leftDownloadButtonController != null) {
					leftDownloadButtonController.buttonPressed();
				}
			}
		});

		// Right download button
		final Button rightDownloadButton = (Button) view.findViewById(R.id.download_button_right);
		rightDownloadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController rightDownloadButtonController = menu.getRightDownloadButtonController();
				if (rightDownloadButtonController != null) {
					rightDownloadButtonController.buttonPressed();
				}
			}
		});

		// Top Right title button
		final Button topRightTitleButton = (Button) view.findViewById(R.id.title_button_top_right);
		topRightTitleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController topRightTitleButtonController = menu.getTopRightTitleButtonController();
				if (topRightTitleButtonController != null) {
					topRightTitleButtonController.buttonPressed();
				}
			}
		});

		// Progress bar
		final ImageView progressButton = (ImageView) view.findViewById(R.id.progressButton);
		progressButton.setImageDrawable(getIcon(R.drawable.ic_action_remove_dark,
				!nightMode ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		progressButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleProgressController titleProgressController = menu.getTitleProgressController();
				if (titleProgressController != null) {
					titleProgressController.buttonPressed();
				}
			}
		});

		menu.updateData();
		updateButtonsAndProgress();

		if (menu.isLandscapeLayout()) {
			final TypedValue typedValueAttr = new TypedValue();
			getMapActivity().getTheme().resolveAttribute(R.attr.left_menu_view_bg, typedValueAttr, true);
			mainView.setBackgroundResource(typedValueAttr.resourceId);
			mainView.setLayoutParams(new FrameLayout.LayoutParams(menu.getLandscapeWidthPx(),
					ViewGroup.LayoutParams.MATCH_PARENT));
			View fabContainer = view.findViewById(R.id.context_menu_fab_container);
			fabContainer.setLayoutParams(new FrameLayout.LayoutParams(menu.getLandscapeWidthPx(),
					ViewGroup.LayoutParams.MATCH_PARENT));
		}

		runLayoutListener();

		final GestureDetector singleTapDetector = new GestureDetector(view.getContext(), new SingleTapConfirm());
		final GestureDetector swipeDetector = new GestureDetector(view.getContext(), new HorizontalSwipeConfirm(true));

		final View.OnTouchListener slideTouchListener = new View.OnTouchListener() {
			private float dy;
			private float dyMain;
			private VelocityTracker velocity;
			private boolean slidingUp;
			private boolean slidingDown;

			private float velocityY;
			private float maxVelocityY;
			private boolean hasMoved;

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (singleTapDetector.onTouchEvent(event)) {
					moving = false;
					if (hasMoved) {
						applyPosY(getViewY(), false, false, 0, 0, 0);
					}
					openMenuHalfScreen();
					return true;
				}

				if (menu.isLandscapeLayout()) {
					if (swipeDetector.onTouchEvent(event)) {
						menu.close();
					}
					return true;
				}

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						hasMoved = false;
						dy = event.getY();
						dyMain = getViewY();
						velocity = VelocityTracker.obtain();
						velocityY = 0;
						maxVelocityY = 0;
						velocity.addMovement(event);
						moving = true;
						break;

					case MotionEvent.ACTION_MOVE:
						if (moving) {
							hasMoved = true;
							float y = event.getY();
							float newY = getViewY() + (y - dy);
							setViewY((int) newY, false, false);

							menuFullHeight = view.getHeight() - (int) newY + 10;
							if (!oldAndroid()) {
								ViewGroup.LayoutParams lp = mainView.getLayoutParams();
								lp.height = Math.max(menuFullHeight, menuTitleHeight);
								mainView.setLayoutParams(lp);
								mainView.requestLayout();
							}

							velocity.addMovement(event);
							velocity.computeCurrentVelocity(1000);
							velocityY = Math.abs(velocity.getYVelocity());
							if (velocityY > maxVelocityY)
								maxVelocityY = velocityY;
						}

						break;

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						if (moving) {
							moving = false;
							int currentY = getViewY();

							slidingUp = Math.abs(maxVelocityY) > 500 && (currentY - dyMain) < -50;
							slidingDown = Math.abs(maxVelocityY) > 500 && (currentY - dyMain) > 50;

							velocity.recycle();

							boolean skipHalfScreenState = Math.abs(currentY - dyMain) > skipHalfScreenStateLimit;
							changeMenuState(currentY, skipHalfScreenState, slidingUp, slidingDown);
						}
						break;

				}
				return true;
			}
		};

		View topView = view.findViewById(R.id.context_menu_top_view);
		topView.setOnTouchListener(slideTouchListener);
		View topShadowView = view.findViewById(R.id.context_menu_top_shadow);
		topShadowView.setOnTouchListener(slideTouchListener);
		View topShadowAllView = view.findViewById(R.id.context_menu_top_shadow_all);
		AndroidUtils.setBackground(getMapActivity(), topShadowAllView, nightMode, R.drawable.bg_map_context_menu_light,
				R.drawable.bg_map_context_menu_dark);
		topShadowAllView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getY() <= dpToPx(SHADOW_HEIGHT_TOP_DP) || event.getAction() != MotionEvent.ACTION_DOWN)
					return slideTouchListener.onTouch(v, event);
				else
					return false;
			}
		});

		buildHeader();

		AndroidUtils.setTextPrimaryColor(getMapActivity(),
				(TextView) view.findViewById(R.id.context_menu_line1), nightMode);
		View menuLine2 = view.findViewById(R.id.context_menu_line2);
		if (menuLine2 != null) {
			AndroidUtils.setTextSecondaryColor(getMapActivity(), (TextView) menuLine2, nightMode);
		}
		((Button) view.findViewById(R.id.title_button_top_right))
				.setTextColor(!nightMode ? getResources().getColor(R.color.map_widget_blue) : getResources().getColor(R.color.osmand_orange));
		AndroidUtils.setTextSecondaryColor(getMapActivity(),
				(TextView) view.findViewById(R.id.distance), nightMode);

		((Button) view.findViewById(R.id.title_button))
				.setTextColor(!nightMode ? getResources().getColor(R.color.map_widget_blue) : getResources().getColor(R.color.osmand_orange));
		AndroidUtils.setTextSecondaryColor(getMapActivity(),
				(TextView) view.findViewById(R.id.title_button_right_text), nightMode);
		((Button) view.findViewById(R.id.title_button_right))
				.setTextColor(!nightMode ? getResources().getColor(R.color.map_widget_blue) : getResources().getColor(R.color.osmand_orange));

		AndroidUtils.setTextSecondaryColor(getMapActivity(),
				(TextView) view.findViewById(R.id.progressTitle), nightMode);

		// FAB
		fabView = (ImageView) view.findViewById(R.id.context_menu_fab_view);
		if (menu.fabVisible()) {
			fabView.setImageDrawable(getIcon(menu.getFabIconId(), 0));
			if (menu.isLandscapeLayout()) {
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) fabView.getLayoutParams();
				params.setMargins(0, 0, dpToPx(28f), 0);
				fabView.setLayoutParams(params);
			}
			fabView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.fabPressed();
				}
			});
		} else {
			fabView.setVisibility(View.GONE);
		}

		// Zoom buttons
		zoomButtonsView = view.findViewById(R.id.context_menu_zoom_buttons);
		zoomInButtonView = (ImageButton) view.findViewById(R.id.context_menu_zoom_in_button);
		zoomOutButtonView = (ImageButton) view.findViewById(R.id.context_menu_zoom_out_button);
		if (menu.zoomButtonsVisible()) {
			updateImageButton(zoomInButtonView, R.drawable.map_zoom_in, R.drawable.map_zoom_in_night,
					R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
			updateImageButton(zoomOutButtonView, R.drawable.map_zoom_out, R.drawable.map_zoom_out_night,
					R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
			zoomInButtonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.zoomInPressed();
				}
			});
			zoomOutButtonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.zoomOutPressed();
				}
			});
			zoomButtonsView.setVisibility(View.VISIBLE);
		} else {
			zoomButtonsView.setVisibility(View.GONE);
		}

		View buttonsTopBorder = view.findViewById(R.id.buttons_top_border);
		AndroidUtils.setBackground(getMapActivity(), buttonsTopBorder, nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		if (!menu.buttonsVisible()) {
			View buttons = view.findViewById(R.id.context_menu_buttons);
			buttonsTopBorder.setVisibility(View.GONE);
			buttons.setVisibility(View.GONE);
		}

		AndroidUtils.setBackground(getMapActivity(), mainView.findViewById(R.id.divider_hor_1), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		AndroidUtils.setBackground(getMapActivity(), mainView.findViewById(R.id.divider_hor_2), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		AndroidUtils.setBackground(getMapActivity(), mainView.findViewById(R.id.divider_hor_3), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);

		// Action buttons
		final ImageButton buttonFavorite = (ImageButton) view.findViewById(R.id.context_menu_fav_button);
		buttonFavorite.setImageDrawable(getIcon(menu.getFavActionIconId(),
				!nightMode ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		AndroidUtils.setDashButtonBackground(getMapActivity(), buttonFavorite, nightMode);
		buttonFavorite.setContentDescription(getString(menu.getFavActionStringId()));
		buttonFavorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonFavoritePressed();
			}
		});

		final ImageButton buttonWaypoint = (ImageButton) view.findViewById(R.id.context_menu_route_button);
		buttonWaypoint.setImageDrawable(getIcon(menu.getWaypointActionIconId(),
				!nightMode ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		buttonWaypoint.setContentDescription(getString(menu.getWaypointActionStringId()));
		AndroidUtils.setDashButtonBackground(getMapActivity(), buttonWaypoint, nightMode);
		buttonWaypoint.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonWaypointPressed();
			}
		});

		final ImageButton buttonShare = (ImageButton) view.findViewById(R.id.context_menu_share_button);
		buttonShare.setImageDrawable(getIcon(R.drawable.map_action_gshare_dark,
				!nightMode ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		AndroidUtils.setDashButtonBackground(getMapActivity(), buttonShare, nightMode);
		buttonShare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonSharePressed();
			}
		});

		final ImageButton buttonMore = (ImageButton) view.findViewById(R.id.context_menu_more_button);
		buttonMore.setImageDrawable(getIcon(R.drawable.map_overflow_menu_white,
				!nightMode ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		AndroidUtils.setDashButtonBackground(getMapActivity(), buttonMore, nightMode);
		buttonMore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonMorePressed();
			}
		});

		buildBottomView();

		view.findViewById(R.id.context_menu_bottom_scroll).setBackgroundColor(nightMode ?
				getResources().getColor(R.color.ctx_menu_info_view_bg_dark) : getResources().getColor(R.color.ctx_menu_info_view_bg_light));
		view.findViewById(R.id.context_menu_bottom_view).setBackgroundColor(nightMode ?
				getResources().getColor(R.color.ctx_menu_info_view_bg_dark) : getResources().getColor(R.color.ctx_menu_info_view_bg_light));

		//getMapActivity().getMapLayers().getMapControlsLayer().setControlsClickable(false);

		if (Build.VERSION.SDK_INT >= 11) {
			containerLayoutListener = new OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View view, int left, int top, int right, int bottom,
										   int oldLeft, int oldTop, int oldRight, int oldBottom) {
					if (bottom != oldBottom) {
						processScreenHeight(view.getParent());
						runLayoutListener();
					}
				}
			};
		}

		created = true;
		return view;
	}

	@Override
	public int getStatusBarColorId() {
		if (menu != null && (menu.getCurrentMenuState() == MenuState.FULL_SCREEN || menu.isLandscapeLayout())) {
			return nightMode ? R.color.status_bar_dark : R.color.status_bar_route_light;
		}
		return -1;
	}

	private void updateImageButton(ImageButton button, int iconLightId, int iconDarkId, int bgLightId, int bgDarkId, boolean night) {
		button.setImageDrawable(getMapActivity().getMyApplication().getIconsCache().getIcon(night ? iconDarkId : iconLightId));
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			button.setBackground(getMapActivity().getResources().getDrawable(night ? bgDarkId : bgLightId,
					getMapActivity().getTheme()));
		} else {
			button.setBackgroundDrawable(getMapActivity().getResources().getDrawable(night ? bgDarkId : bgLightId));
		}
	}

	private void processScreenHeight(ViewParent parent) {
		View container = (View)parent;
		if (Build.VERSION.SDK_INT >= 11) {
			screenHeight = container.getHeight() + AndroidUtils.getStatusBarHeight(getActivity());
		} else {
			screenHeight = AndroidUtils.getScreenHeight(getActivity());
		}
		skipHalfScreenStateLimit = screenHeight * SKIP_HALF_SCREEN_STATE_KOEF;
		viewHeight = screenHeight - AndroidUtils.getStatusBarHeight(getMapActivity());
	}

	public void openMenuFullScreen() {
		changeMenuState(getViewY(), true, true, false);
	}

	public void openMenuHalfScreen() {
		int oldMenuState = menu.getCurrentMenuState();
		if (oldMenuState == MenuState.HEADER_ONLY) {
			changeMenuState(getViewY(), false, true, false);
		} else if (oldMenuState == MenuState.FULL_SCREEN && !menu.isLandscapeLayout()) {
			changeMenuState(getViewY(), false, false, true);
		}
	}

	private void changeMenuState(int currentY, boolean skipHalfScreenState,
								 boolean slidingUp, boolean slidingDown) {
		boolean needCloseMenu = false;

		int oldMenuState = menu.getCurrentMenuState();
		if (menuBottomViewHeight > 0 && slidingUp) {
			menu.slideUp();
			if (skipHalfScreenState) {
				menu.slideUp();
			}
		} else if (slidingDown) {
			needCloseMenu = !menu.slideDown();
			if (!needCloseMenu && skipHalfScreenState) {
				menu.slideDown();
			}
		}
		int newMenuState = menu.getCurrentMenuState();
		boolean needMapAdjust = oldMenuState != newMenuState && newMenuState != MenuState.FULL_SCREEN;

		if (newMenuState != oldMenuState) {
			restoreCustomMapRatio();
			menu.updateControlsVisibility(true);
			doBeforeMenuStateChange(oldMenuState, newMenuState);
		}

		applyPosY(currentY, needCloseMenu, needMapAdjust, oldMenuState, newMenuState, 0);
	}

	private void restoreCustomMapRatio() {
		if (map != null && map.hasCustomMapRatio()) {
			map.restoreMapRatio();
		}
	}

	private void setCustomMapRatio() {
		LatLon latLon = menu.getLatLon();
		RotatedTileBox tb = map.getCurrentRotatedTileBox().copy();
		float px = tb.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
		float py = tb.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
		float ratioX = px / tb.getPixWidth();
		float ratioY = py / tb.getPixHeight();
		map.setCustomMapRatio(ratioX, ratioY);
		map.setLatLon(latLon.getLatitude(), latLon.getLongitude());
	}

	public void doZoomIn() {
		if (map.isZooming() && map.hasCustomMapRatio()) {
			getMapActivity().changeZoom(2, System.currentTimeMillis());
		} else {
			if (!map.hasCustomMapRatio()) {
				setCustomMapRatio();
			}
			getMapActivity().changeZoom(1, System.currentTimeMillis());
		}
	}

	public void doZoomOut() {
		if (!map.hasCustomMapRatio()) {
			setCustomMapRatio();
		}
		getMapActivity().changeZoom(-1, System.currentTimeMillis());
	}

	private void applyPosY(final int currentY, final boolean needCloseMenu, boolean needMapAdjust,
						   final int previousMenuState, final int newMenuState, int dZoom) {
		final int posY = getPosY(needCloseMenu);
		if (currentY != posY || dZoom != 0) {
			if (posY < currentY) {
				updateMainViewLayout(posY);
			}

			if (!oldAndroid()) {
				mainView.animate().y(posY)
						.setDuration(200)
						.setInterpolator(new DecelerateInterpolator())
						.setListener(new AnimatorListenerAdapter() {

							boolean canceled = false;

							@Override
							public void onAnimationCancel(Animator animation) {
								canceled = true;
							}

							@Override
							public void onAnimationEnd(Animator animation) {
								if (!canceled) {
									if (needCloseMenu) {
										menu.close();
									} else {
										updateMainViewLayout(posY);
										if (previousMenuState != 0 && newMenuState != 0 && previousMenuState != newMenuState) {
											doAfterMenuStateChange(previousMenuState, newMenuState);
										}
									}
								}
							}
						})
						.start();

				fabView.animate().y(getFabY(posY))
						.setDuration(200)
						.setInterpolator(new DecelerateInterpolator())
						.start();

				zoomButtonsView.animate().y(getZoomButtonsY(posY))
						.setDuration(200)
						.setInterpolator(new DecelerateInterpolator())
						.start();

				if (needMapAdjust) {
					adjustMapPosition(posY, true, centered, dZoom);
				}
			} else {
				setViewY(posY, false, needMapAdjust);
				if (needCloseMenu) {
					menu.close();
				} else {
					updateMainViewLayout(posY);
					if (previousMenuState != 0 && newMenuState != 0 && previousMenuState != newMenuState) {
						doAfterMenuStateChange(previousMenuState, newMenuState);
					}
				}
			}
		}
	}

	public void updateMapCenter(LatLon mapCenter) {
		customMapCenter = true;
		menu.setMapCenter(mapCenter);
		this.mapCenter = mapCenter;
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		origMarkerX = box.getCenterPixelX();
		origMarkerY = box.getCenterPixelY();
	}

	private void updateButtonsAndProgress() {
		if (view != null) {
			TitleButtonController leftTitleButtonController = menu.getLeftTitleButtonController();
			TitleButtonController rightTitleButtonController = menu.getRightTitleButtonController();
			TitleButtonController topRightTitleButtonController = menu.getTopRightTitleButtonController();
			TitleButtonController leftSubtitleButtonController = menu.getLeftSubtitleButtonController();
			TitleButtonController leftDownloadButtonController = menu.getLeftDownloadButtonController();
			TitleButtonController rightDownloadButtonController = menu.getRightDownloadButtonController();
			TitleProgressController titleProgressController = menu.getTitleProgressController();

			// Title buttons
			boolean showTitleButtonsContainer = (leftTitleButtonController != null || rightTitleButtonController != null);
			boolean showTitleDivider = leftSubtitleButtonController != null;
			final View titleButtonsContainer = view.findViewById(R.id.title_button_container);
			titleButtonsContainer.setVisibility(showTitleButtonsContainer ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.title_divider).setVisibility(showTitleDivider ? View.VISIBLE : View.GONE);

			// Left title button
			final Button leftTitleButton = (Button) view.findViewById(R.id.title_button);
			final TextView titleButtonRightText = (TextView) view.findViewById(R.id.title_button_right_text);
			if (leftTitleButtonController != null) {
				leftTitleButton.setText(leftTitleButtonController.caption);
				leftTitleButton.setVisibility(leftTitleButtonController.visible ? View.VISIBLE : View.GONE);

				Drawable leftIcon = leftTitleButtonController.getLeftIcon();
				if (leftIcon != null) {
					leftTitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, null, null);
					leftTitleButton.setCompoundDrawablePadding(dpToPx(8f));
				}

				if (leftTitleButtonController.needRightText) {
					titleButtonRightText.setText(leftTitleButtonController.rightTextCaption);
					titleButtonRightText.setVisibility(View.VISIBLE);
				} else {
					titleButtonRightText.setVisibility(View.GONE);
				}
			} else {
				leftTitleButton.setVisibility(View.GONE);
				titleButtonRightText.setVisibility(View.GONE);
			}

			// Right title button
			final Button rightTitleButton = (Button) view.findViewById(R.id.title_button_right);
			if (rightTitleButtonController != null) {
				rightTitleButton.setText(rightTitleButtonController.caption);
				rightTitleButton.setVisibility(rightTitleButtonController.visible ? View.VISIBLE : View.GONE);

				Drawable leftIcon = rightTitleButtonController.getLeftIcon();
				rightTitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, null, null);
				rightTitleButton.setCompoundDrawablePadding(dpToPx(8f));
			} else {
				rightTitleButton.setVisibility(View.GONE);
			}

			// Top Right title button
			final Button topRightTitleButton = (Button) view.findViewById(R.id.title_button_top_right);
			if (topRightTitleButtonController != null) {
				topRightTitleButton.setText(topRightTitleButtonController.caption);
				topRightTitleButton.setVisibility(topRightTitleButtonController.visible ? View.VISIBLE : View.INVISIBLE);

				Drawable leftIcon = topRightTitleButtonController.getLeftIcon();
				topRightTitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, null, null);
				topRightTitleButton.setCompoundDrawablePadding(dpToPx(8f));
			} else {
				topRightTitleButton.setVisibility(View.GONE);
			}

			// Left subtitle button
			final Button leftSubtitleButton = (Button) view.findViewById(R.id.subtitle_button);
			if (leftSubtitleButtonController != null) {
				leftSubtitleButton.setText(leftSubtitleButtonController.caption);
				leftSubtitleButton.setVisibility(leftSubtitleButtonController.visible ? View.VISIBLE : View.GONE);

				Drawable leftIcon = leftSubtitleButtonController.getLeftIcon();
				if (leftIcon != null) {
					leftSubtitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, null, null);
					leftSubtitleButton.setCompoundDrawablePadding(dpToPx(8f));
				}
			} else {
				leftSubtitleButton.setVisibility(View.GONE);
			}

			// Download buttons
			boolean showDownloadButtonsContainer =
					((leftDownloadButtonController != null && leftDownloadButtonController.visible)
							|| (rightDownloadButtonController != null && rightDownloadButtonController.visible))
							&& (titleProgressController == null || !titleProgressController.visible);
			final View downloadButtonsContainer = view.findViewById(R.id.download_buttons_container);
			downloadButtonsContainer.setVisibility(showDownloadButtonsContainer ? View.VISIBLE : View.GONE);

			if (showDownloadButtonsContainer) {
				view.findViewById(R.id.download_buttons_top_border).setVisibility(showTitleButtonsContainer ? View.VISIBLE : View.INVISIBLE);
				if (showTitleButtonsContainer) {
					LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams) downloadButtonsContainer.getLayoutParams();
					if (ll.topMargin != 0) {
						ll.setMargins(0, 0, 0, 0);
					}
				}
			}

			// Left download button
			final Button leftDownloadButton = (Button) view.findViewById(R.id.download_button_left);
			if (leftDownloadButtonController != null) {
				leftDownloadButton.setText(leftDownloadButtonController.caption);
				leftDownloadButton.setVisibility(leftDownloadButtonController.visible ? View.VISIBLE : View.GONE);

				Drawable leftIcon = leftDownloadButtonController.getLeftIcon();
				if (leftIcon != null) {
					leftDownloadButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, null, null);
					leftDownloadButton.setCompoundDrawablePadding(dpToPx(8f));
				}
			} else {
				leftDownloadButton.setVisibility(View.GONE);
			}

			// Right download button
			final Button rightDownloadButton = (Button) view.findViewById(R.id.download_button_right);
			if (rightDownloadButtonController != null) {
				rightDownloadButton.setText(rightDownloadButtonController.caption);
				rightDownloadButton.setVisibility(rightDownloadButtonController.visible ? View.VISIBLE : View.GONE);

				Drawable leftIcon = rightDownloadButtonController.getLeftIcon();
				rightDownloadButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, null, null);
				rightDownloadButton.setCompoundDrawablePadding(dpToPx(8f));
			} else {
				rightDownloadButton.setVisibility(View.GONE);
			}

			// Progress bar
			final View titleProgressContainer = view.findViewById(R.id.title_progress_container);
			if (titleProgressController != null) {
				titleProgressContainer.setVisibility(titleProgressController.visible ? View.VISIBLE : View.GONE);
				if (titleProgressController.visible && showTitleButtonsContainer) {
					LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams) titleProgressContainer.getLayoutParams();
					if (ll.topMargin != 0) {
						ll.setMargins(0, 0, 0, 0);
					}
				}

				final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
				final TextView progressTitle = (TextView) view.findViewById(R.id.progressTitle);
				progressTitle.setText(titleProgressController.caption);
				progressBar.setIndeterminate(titleProgressController.indeterminate);
				progressBar.setProgress(titleProgressController.progress);
				progressBar.setVisibility(titleProgressController.progressVisible ? View.VISIBLE : View.GONE);

				final ImageView progressButton = (ImageView) view.findViewById(R.id.progressButton);
				progressButton.setVisibility(titleProgressController.buttonVisible ? View.VISIBLE : View.GONE);
			} else {
				titleProgressContainer.setVisibility(View.GONE);
			}
		}
	}

	private void buildHeader() {
		OsmandApplication app = getMyApplication();
		if (app != null && view != null) {
			final View iconLayout = view.findViewById(R.id.context_menu_icon_layout);
			final ImageView iconView = (ImageView) view.findViewById(R.id.context_menu_icon_view);
			Drawable icon = menu.getLeftIcon();
			int iconId = menu.getLeftIconId();
			if (icon != null) {
				iconView.setImageDrawable(icon);
				iconLayout.setVisibility(View.VISIBLE);
			} else if (iconId != 0) {
				iconView.setImageDrawable(getIcon(iconId,
						!nightMode ? R.color.osmand_orange : R.color.osmand_orange_dark));
				iconLayout.setVisibility(View.VISIBLE);
			} else {
				iconLayout.setVisibility(View.GONE);
			}
			setAddressLocation();
		}
	}

	private void buildBottomView() {
		if (view != null) {
			View bottomView = view.findViewById(R.id.context_menu_bottom_view);
			if (menu.isExtended()) {
				bottomView.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						return true;
					}
				});
				menu.build(bottomView);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!menu.isActive() || MapRouteInfoMenu.isVisible()) {
			dismissMenu();
			return;
		}
		screenOrientation = DashLocationFragment.getScreenOrientation(getActivity());
		getMapActivity().getMapViewTrackingUtilities().setContextMenu(menu);
		getMapActivity().getMapViewTrackingUtilities().setMapLinkedToLocation(false);
		wasDrawerDisabled = getMapActivity().isDrawerDisabled();
		if (!wasDrawerDisabled) {
			getMapActivity().disableDrawer();
		}
		ViewParent parent = view.getParent();
		if (parent != null && containerLayoutListener != null) {
			((View) parent).addOnLayoutChangeListener(containerLayoutListener);
		}
		menu.updateControlsVisibility(true);
		getMapActivity().getMapLayers().getMapControlsLayer().showMapControls();
	}

	@Override
	public void onPause() {
		restoreCustomMapRatio();

		if (view != null) {
			ViewParent parent = view.getParent();
			if (parent != null && containerLayoutListener != null) {
				((View) parent).removeOnLayoutChangeListener(containerLayoutListener);
			}
			getMapActivity().getMapViewTrackingUtilities().setContextMenu(null);
			getMapActivity().getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			if (!wasDrawerDisabled) {
				getMapActivity().enableDrawer();
			}
			menu.updateControlsVisibility(false);
		}
		super.onPause();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (!menu.isActive()) {
			if (mapCenter != null) {
				if (mapZoom == 0) {
					mapZoom = map.getZoom();
				}
				//map.setLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
				//map.setIntZoom(mapZoom);
				AnimateDraggingMapThread thread = map.getAnimatedDraggingThread();
				thread.startMoving(mapCenter.getLatitude(), mapCenter.getLongitude(), mapZoom, true);
			}
		}
		menu.setMapCenter(null);
		menu.setMapZoom(0);
	}

	public void rebuildMenu(boolean centered) {
		OsmandApplication app = getMyApplication();
		if (app != null && view != null) {
			final ImageButton buttonFavorite = (ImageButton) view.findViewById(R.id.context_menu_fav_button);
			buttonFavorite.setImageDrawable(getIcon(menu.getFavActionIconId(),
					!nightMode ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
			buttonFavorite.setContentDescription(getString(menu.getFavActionStringId()));

			buildHeader();

			LinearLayout bottomLayout = (LinearLayout) view.findViewById(R.id.context_menu_bottom_view);
			bottomLayout.removeAllViews();
			buildBottomView();

			if (centered) {
				this.initLayout = true;
				this.centered = true;
			}
			runLayoutListener();
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void runLayoutListener() {
		if (view != null) {
			ViewTreeObserver vto = view.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {

					if (view != null) {
						ViewTreeObserver obs = view.getViewTreeObserver();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							obs.removeOnGlobalLayoutListener(this);
						} else {
							obs.removeGlobalOnLayoutListener(this);
						}

						if (getActivity() == null) {
							return;
						}

						int newMenuTopViewHeight = view.findViewById(R.id.context_menu_top_view).getHeight();
						menuTopShadowHeight = view.findViewById(R.id.context_menu_top_shadow).getHeight();
						int newMenuTopShadowAllHeight = view.findViewById(R.id.context_menu_top_shadow_all).getHeight();
						menuFullHeight = view.findViewById(R.id.context_menu_main).getHeight();
						zoomButtonsHeight = zoomButtonsView.getHeight();

						int dy = 0;
						if (!menu.isLandscapeLayout()) {
							TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
							TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
							int line2LineCount = 0;
							int line2LineHeight = 0;
							int line2MeasuredHeight = 0;
							if (line2 != null) {
								line2LineCount = line2.getLineCount();
								line2LineHeight = line2.getLineHeight();
								line2MeasuredHeight = line2.getMeasuredHeight();
							}

							int dp16 = dpToPx(16f);
							boolean has16margin = false;
							int titleButtonHeight = 0;
							View titleButtonContainer = view.findViewById(R.id.title_button_container);
							if (titleButtonContainer.getVisibility() == View.VISIBLE) {
								titleButtonHeight = titleButtonContainer.getMeasuredHeight() - dp16;
								if (titleButtonHeight < 0) {
									titleButtonHeight = 0;
								} else {
									has16margin = true;
								}
							}
							int downloadButtonsHeight = 0;
							View downloadButtonsContainer = view.findViewById(R.id.download_buttons_container);
							if (downloadButtonsContainer.getVisibility() == View.VISIBLE) {
								downloadButtonsHeight = downloadButtonsContainer.getMeasuredHeight() - (has16margin ? 0 : dp16);
								if (downloadButtonsHeight < 0) {
									downloadButtonsHeight = 0;
								} else {
									has16margin = true;
								}
							}
							int titleProgressHeight = 0;
							View titleProgressContainer = view.findViewById(R.id.title_progress_container);
							if (titleProgressContainer.getVisibility() == View.VISIBLE) {
								titleProgressHeight = titleProgressContainer.getMeasuredHeight() - (has16margin ? 0 : dp16);
								if (titleProgressHeight < 0) {
									titleProgressHeight = 0;
								}
							}

							if (menuTopViewHeight != 0) {
								int titleHeight = line1.getLineCount() * line1.getLineHeight() + line2LineCount * line2LineHeight + menuTitleTopBottomPadding;
								if (titleHeight < line1.getMeasuredHeight() + line2MeasuredHeight) {
									titleHeight = line1.getMeasuredHeight() + line2MeasuredHeight;
								}
								newMenuTopViewHeight = menuTopViewHeightExcludingTitle + titleHeight + titleButtonHeight + downloadButtonsHeight + titleProgressHeight;
								dy = Math.max(0, newMenuTopViewHeight - menuTopViewHeight - (newMenuTopShadowAllHeight - menuTopShadowAllHeight));
							} else {
								menuTopViewHeightExcludingTitle = newMenuTopViewHeight - line1.getMeasuredHeight() - line2MeasuredHeight - titleButtonHeight - downloadButtonsHeight - titleProgressHeight;
								menuTitleTopBottomPadding = (line1.getMeasuredHeight() - line1.getLineCount() * line1.getLineHeight())
										+ (line2MeasuredHeight - line2LineCount * line2LineHeight);
							}
						}
						menuTopViewHeight = newMenuTopViewHeight;
						menuTopShadowAllHeight = newMenuTopShadowAllHeight;
						menuTitleHeight = menuTopShadowHeight + menuTopShadowAllHeight + dy;
						menuBottomViewHeight = view.findViewById(R.id.context_menu_bottom_view).getHeight();

						menuFullHeightMax = menuTitleHeight + menuBottomViewHeight;

						if (origMarkerX == 0 && origMarkerY == 0) {
							origMarkerX = view.getWidth() / 2;
							origMarkerY = view.getHeight() / 2;
						}

						if (initLayout && centered) {
							centerMarkerLocation();
						}
						if (!moving) {
							doLayoutMenu();
						}
						initLayout = false;
					}
				}

			});
		}
	}

	public void centerMarkerLocation() {
		centered = true;
		showOnMap(menu.getLatLon(), true, true, false, getZoom());
	}

	private int getZoom() {
		int zoom;
		if (zoomIn) {
			zoom = ZOOM_IN_STANDARD;
		} else {
			zoom = menu.getMapZoom();
		}
		if (zoom == 0) {
			zoom = map.getZoom();
		}
		return zoom;
	}

	private LatLon calculateCenterLatLon(LatLon latLon, int zoom, boolean updateOrigXY) {
		double flat = latLon.getLatitude();
		double flon = latLon.getLongitude();

		RotatedTileBox cp = map.getCurrentRotatedTileBox().copy();
		cp.setCenterLocation(0.5f, map.getMapPosition() == OsmandSettings.BOTTOM_CONSTANT ? 0.15f : 0.5f);
		cp.setLatLonCenter(flat, flon);
		cp.setZoom(zoom);
		flat = cp.getLatFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);
		flon = cp.getLonFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);

		if (updateOrigXY) {
			origMarkerX = cp.getCenterPixelX();
			origMarkerY = cp.getCenterPixelY();
		}
		return new LatLon(flat, flon);
	}

	private void showOnMap(LatLon latLon, boolean updateCoords, boolean needMove, boolean alreadyAdjusted, int zoom) {
		AnimateDraggingMapThread thread = map.getAnimatedDraggingThread();
		LatLon calcLatLon = calculateCenterLatLon(latLon, zoom, updateCoords);
		if (updateCoords) {
			mapCenter = calcLatLon;
			menu.setMapCenter(mapCenter);
		}

		if (!alreadyAdjusted) {
			calcLatLon = getAdjustedMarkerLocation(getPosY(), calcLatLon, true, zoom);
		}

		if (needMove) {
			thread.startMoving(calcLatLon.getLatitude(), calcLatLon.getLongitude(), zoom, true);
		}
	}

	private void setAddressLocation() {
		if (view != null) {
			// Text line 1
			TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
			line1.setText(menu.getTitleStr());

			// Text line 2
			LinearLayout line2layout = (LinearLayout) view.findViewById(R.id.context_menu_line2_layout);
			TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
			if (menu.hasCustomAddressLine()) {
				line2layout.removeAllViews();
				menu.buildCustomAddressLine(line2layout);
			} else {
				String typeStr = menu.getTypeStr();
				String streetStr = menu.getStreetStr();
				StringBuilder line2Str = new StringBuilder();
				if (!Algorithms.isEmpty(typeStr)) {
					line2Str.append(typeStr);
					Drawable icon = menu.getTypeIcon();
					line2.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
					line2.setCompoundDrawablePadding(dpToPx(5f));
				}
				if (!Algorithms.isEmpty(streetStr) && !menu.displayStreetNameInTitle()) {
					if (line2Str.length() > 0) {
						line2Str.append(": ");
					}
					line2Str.append(streetStr);
				}
				line2.setText(line2Str.toString());
			}
		}
		updateCompassVisibility();
	}

	private void updateCompassVisibility() {
		OsmandApplication app = getMyApplication();
		if (app != null && view != null) {
			View compassView = view.findViewById(R.id.compass_layout);
			Location ll = app.getLocationProvider().getLastKnownLocation();
			if (ll != null && menu.displayDistanceDirection() && menu.getCurrentMenuState() != MenuState.FULL_SCREEN) {
				updateDistanceDirection();
				compassView.setVisibility(View.VISIBLE);
			} else {
				if (!menu.displayDistanceDirection()) {
					compassView.setVisibility(View.GONE);
				} else {
					compassView.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	private void updateDistanceDirection() {
		OsmandApplication app = getMyApplication();
		FragmentActivity activity = getActivity();
		if (app != null && activity != null && view != null) {
			TextView distanceText = (TextView) view.findViewById(R.id.distance);
			ImageView direction = (ImageView) view.findViewById(R.id.direction);
			float myHeading = menu.getHeading() == null ? 0f : menu.getHeading();
			DashLocationFragment.updateLocationView(false, menu.getMyLocation(), myHeading, direction, distanceText,
					menu.getLatLon().getLatitude(), menu.getLatLon().getLongitude(), screenOrientation, app, activity);
		}
	}

	private int getPosY() {
		return getPosY(false);
	}

	private int getPosY(boolean needCloseMenu) {
		if (needCloseMenu) {
			return screenHeight;
		}

		int destinationState;
		int minHalfY;
		if (menu.isExtended()) {
			destinationState = menu.getCurrentMenuState();
			minHalfY = viewHeight - (int) (viewHeight * menu.getHalfScreenMaxHeightKoef());
		} else {
			destinationState = MenuState.HEADER_ONLY;
			minHalfY = viewHeight;
		}

		int posY = 0;
		switch (destinationState) {
			case MenuState.HEADER_ONLY:
				posY = viewHeight - menuTitleHeight;
				break;
			case MenuState.HALF_SCREEN:
				posY = viewHeight - menuFullHeightMax;
				posY = Math.max(posY, minHalfY);
				break;
			case MenuState.FULL_SCREEN:
				posY = -menuTopShadowHeight - dpToPx(SHADOW_HEIGHT_TOP_DP);
				posY = addStatusBarHeightIfNeeded(posY);
				break;
			default:
				break;
		}
		if (!menu.isLandscapeLayout()) {
			getMapActivity().updateStatusBarColor();
		}
		return posY;
	}

	private int addStatusBarHeightIfNeeded(int res) {
		if (Build.VERSION.SDK_INT >= 21) {
			// One pixel is needed to fill a thin gap between the status bar and the fragment.
			return res + AndroidUtils.getStatusBarHeight(getActivity()) - 1;
		}
		return res;
	}

	private void updateMainViewLayout(int posY) {
		if (view != null) {
			menuFullHeight = view.getHeight() - posY;
			if (!oldAndroid()) {
				ViewGroup.LayoutParams lp = mainView.getLayoutParams();
				lp.height = Math.max(menuFullHeight, menuTitleHeight);
				mainView.setLayoutParams(lp);
				mainView.requestLayout();
			}
		}
	}

	private int getViewY() {
		if (!oldAndroid()) {
			return (int) mainView.getY();
		} else {
			return mainView.getPaddingTop();
		}
	}

	private void setViewY(int y, boolean animated, boolean adjustMapPos) {
		if (!oldAndroid()) {
			mainView.setY(y);
			fabView.setY(getFabY(y));
			zoomButtonsView.setY(getZoomButtonsY(y));
		} else {
			mainView.setPadding(0, y, 0, 0);
			fabView.setPadding(0, getFabY(y), 0, 0);
			zoomButtonsView.setPadding(0, getZoomButtonsY(y), 0, 0);
		}
		if (!customMapCenter) {
			if (adjustMapPos) {
				adjustMapPosition(y, animated, centered, 0);
			}
		} else {
			customMapCenter = false;
		}
	}

	private void adjustMapPosition(int y, boolean animated, boolean center, int dZoom) {
		map.getAnimatedDraggingThread().stopAnimatingSync();
		int zoom = getZoom() + dZoom;
		LatLon latlon = getAdjustedMarkerLocation(y, menu.getLatLon(), center, zoom);

		if (map.getLatitude() == latlon.getLatitude() && map.getLongitude() == latlon.getLongitude() && dZoom == 0) {
			return;
		}

		if (animated) {
			showOnMap(latlon, false, true, true, zoom);
		} else {
			if (dZoom != 0) {
				map.setIntZoom(zoom);
			}
			map.setLatLon(latlon.getLatitude(), latlon.getLongitude());
		}
	}

	private LatLon getAdjustedMarkerLocation(int y, LatLon reqMarkerLocation, boolean center, int zoom) {
		double markerLat = reqMarkerLocation.getLatitude();
		double markerLon = reqMarkerLocation.getLongitude();
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		box.setCenterLocation(0.5f, map.getMapPosition() == OsmandSettings.BOTTOM_CONSTANT ? 0.15f : 0.5f);
		box.setZoom(zoom);
		boolean hasMapCenter = mapCenter != null;
		int markerMapCenterX = 0;
		int markerMapCenterY = 0;
		if (hasMapCenter) {
			markerMapCenterX = (int) box.getPixXFromLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
			markerMapCenterY = (int) box.getPixYFromLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
		}
		float cpyOrig = box.getCenterPixelPoint().y;

		box.setCenterLocation(0.5f, 0.5f);
		int markerX = (int) box.getPixXFromLatLon(markerLat, markerLon);
		int markerY = (int) box.getPixYFromLatLon(markerLat, markerLon);
		QuadPoint cp = box.getCenterPixelPoint();
		float cpx = cp.x;
		float cpy = cp.y;

		float cpyDelta = menu.isLandscapeLayout() ? 0 : cpyOrig - cpy;

		markerY += cpyDelta;
		y += cpyDelta;
		float origMarkerY = this.origMarkerY + cpyDelta;

		LatLon latlon;
		if (center || !hasMapCenter) {
			latlon = reqMarkerLocation;
		} else {
			latlon = box.getLatLonFromPixel(markerMapCenterX, markerMapCenterY);
		}
		if (menu.isLandscapeLayout()) {
			int x = menu.getLandscapeWidthPx();
			if (markerX - markerPaddingXPx < x || markerX > origMarkerX) {
				int dx = (x + markerPaddingXPx) - markerX;
				int dy = 0;
				if (center) {
					dy = (int) cpy - markerY;
				} else {
					cpy = cpyOrig;
				}
				if (dx >= 0 || center) {
					latlon = box.getLatLonFromPixel(cpx - dx, cpy - dy);
				}
			}
		} else {
			if (markerY + markerPaddingPx > y || markerY < origMarkerY) {
				int dx = 0;
				int dy = markerY - (y - markerPaddingPx);
				if (markerY - dy <= origMarkerY) {
					if (center) {
						dx = markerX - (int) cpx;
					}
					latlon = box.getLatLonFromPixel(cpx + dx, cpy + dy);
				}
			}
		}
		return latlon;
	}

	private int getFabY(int y) {
		int fabY = y + fabPaddingTopPx;
		if (fabY < fabPaddingTopPx) {
			fabY = fabPaddingTopPx;
			fabY = addStatusBarHeightIfNeeded(fabY);
		}
		return fabY;
	}

	private int getZoomButtonsY(int y) {
		return y - zoomButtonsHeight - fabPaddingTopPx;
	}

	private boolean oldAndroid() {
		return (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	}

	private void doLayoutMenu() {
		final int posY = getPosY();
		setViewY(posY, true, !initLayout || !centered);
		updateMainViewLayout(posY);
	}

	public void dismissMenu() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			try {
				activity.getSupportFragmentManager().popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
			}
		}
	}

	public void refreshTitle() {
		setAddressLocation();
		runLayoutListener();
	}

	public void setFragmentVisibility(boolean visible) {
		if (view != null) {
			if (visible) {
				view.setVisibility(View.VISIBLE);
				if (mapCenter != null) {
					map.setLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
				}
				adjustMapPosition(getPosY(), true, false, 0);
			} else {
				view.setVisibility(View.GONE);
			}
		}
	}

	public OsmandApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}

	public static boolean showInstance(final MapContextMenu menu, final MapActivity mapActivity,
									   final boolean centered) {
		try {

			if (menu.getLatLon() == null || mapActivity.isActivityDestroyed()) {
				return false;
			}

			int slideInAnim = 0;
			int slideOutAnim = 0;
			if (!mapActivity.getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				slideInAnim = R.anim.slide_in_bottom;
				slideOutAnim = R.anim.slide_out_bottom;

				if (menu.isExtended()) {
					slideInAnim = menu.getSlideInAnimation();
					slideOutAnim = menu.getSlideOutAnimation();
				}
			}

			MapContextMenuFragment fragment = new MapContextMenuFragment();
			fragment.centered = centered;
			mapActivity.getSupportFragmentManager().beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG).commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	//DownloadEvents
	@Override
	public void newDownloadIndexes() {
		updateOnDownload();
	}

	@Override
	public void downloadInProgress() {
		updateOnDownload();
	}

	@Override
	public void downloadHasFinished() {
		updateOnDownload();
		if (menu != null && menu.isVisible() && menu.isMapDownloaded()) {
			rebuildMenu(false);
		}
	}

	private void updateOnDownload() {
		if (menu != null) {
			boolean wasProgressVisible = menu.getTitleProgressController() != null && menu.getTitleProgressController().visible;
			menu.updateData();
			boolean progressVisible = menu.getTitleProgressController() != null && menu.getTitleProgressController().visible;
			updateButtonsAndProgress();
			if (wasProgressVisible != progressVisible) {
				runLayoutListener();
			}
		}
	}

	public void updateMenu() {
		if (created) {
			menu.updateData();
			updateButtonsAndProgress();
			runLayoutListener();
		}
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private int dpToPx(float dp) {
		Resources r = getActivity().getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	public void updateLocation(boolean centerChanged, boolean locationChanged, boolean compassChanged) {
		updateDistanceDirection();
	}

	private void doBeforeMenuStateChange(int previousState, int newState) {
		if (newState == MenuState.HALF_SCREEN) {
			centered = true;
			if (!zoomIn && menu.supportZoomIn()) {
				if (getZoom() < ZOOM_IN_STANDARD) {
					zoomIn = true;
				}
			}
			calculateCenterLatLon(menu.getLatLon(), getZoom(), true);
		}
	}

	private void doAfterMenuStateChange(int previousState, int newState) {
		updateCompassVisibility();
	}
}

