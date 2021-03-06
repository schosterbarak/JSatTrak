/*
 * =====================================================================
 * Copyright (C) 2009 Shawn E. Gano
 *
 * This file is part of JSatTrak.
 *
 * JSatTrak is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JSatTrak is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JSatTrak.  If not, see <http://www.gnu.org/licenses/>.
 * =====================================================================
 * View to orbit any point/position (for instance to view a model)
 * (Earth Inertial View Reference Frame)
 * Shawn Gano
 */

package name.gano.worldwind.view;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.AbstractView;
import gov.nasa.worldwind.view.ViewSupport;
import javax.media.opengl.GL;



/**
 * @author dcollins
 * @version $Id: BasicOrbitView.java 3557 2007-11-17 04:10:32Z dcollins $
 */
public class BasicModelView extends AbstractView //implements OrbitView
{
    // View attributes.
    private BasicModelViewModel basicModelViewModel;
    private Frustum frustum = new Frustum();//null;
    
     private Globe globe;
     
     private Position iniPosition;
     
     double minClipDist = 100; // 900000
     
     // new 
     private double nearClipDistance = -1; // Default to auto-configure.
    private double farClipDistance = -1;  // Default to auto-configure.
    private java.awt.Rectangle viewport = new java.awt.Rectangle();
    private Angle fieldOfView = Angle.fromDegrees(45);
    // Properties updated during the most recent call to apply().
    private DrawContext dc;
    private final ViewSupport viewSupport = new ViewSupport();
    // TODO: make configurable
    private static final double MINIMUM_NEAR_DISTANCE = 2;
    private static final double MINIMUM_FAR_DISTANCE = 100;

     // constructors
    public BasicModelView(Position centerPosition, Globe globe)
    {
        this.globe = globe;
        
        iniPosition = centerPosition; // save for inialization  
    }
    
     public void setCenterPosition(Position centerPosition)
    {
        Vec4 v4 = globe.computePointFromPosition(centerPosition);
        
        double[] centerPos = new double[3];
        centerPos[0] = v4.getX();
        centerPos[1] = v4.getY();
        centerPos[2] = v4.getZ();
        
        basicModelViewModel.setCenterPos(centerPos);
    }
    
    
    // ============== Viewing State ======================= 
    // updated in input broker
    
    public void addToTheta1(float Dtheta1)
    {
        basicModelViewModel.addToTheta1(Dtheta1);
    }
    
    public void addToTheta2(float Dtheta2)
    {
        basicModelViewModel.addToTheta2(Dtheta2);
    }
    
    public void addToTheta3(float Dtheta3)
    {        
        basicModelViewModel.addToTheta3(Dtheta3);
    }
    
    public void addToRadiusAboutCenter(float meters)
    {
        basicModelViewModel.addToRadiusAboutCenter(meters);
    }
    
    public void addToXOffsetAboutCenter(float meters)
    {
        basicModelViewModel.addToXOffsetAboutCenter(meters);
    }
    
    public void addToYOffsetAboutCenter(float meters)
    {
        basicModelViewModel.addToYOffsetAboutCenter(meters);
    }
    
    public void addToZOffsetAboutCenter(float meters)
    {
        basicModelViewModel.addToZOffsetAboutCenter(meters);
    }
    
    //================================================
    
    protected void doApply(DrawContext dc)
    {
        if (this.basicModelViewModel == null)
            return;

        Vec4 eyeVec = this.basicModelViewModel.getEyeVector();
        if (eyeVec == null)
            return;

        // Update DrawContext and Globe references.
        this.dc = dc;
        this.globe = this.dc.getGlobe();
        
        // Get the current OpenGL viewport state.
        int[] viewportArray = new int[4];
        dc.getGL().glGetIntegerv(GL.GL_VIEWPORT, viewportArray, 0);
        this.viewport = new java.awt.Rectangle(viewportArray[0], viewportArray[1], viewportArray[2], viewportArray[3]);
        
        // Compute current viewing attributes.
        double nearDistance = this.nearClipDistance > 0 ? this.nearClipDistance : getAutoNearClipDistance();
        double farDistance = this.farClipDistance > 0 ? this.farClipDistance : getAutoFarClipDistance();
        
        this.frustum = this.createFrustum(nearDistance, farDistance);

        // SEG  -- creakte ransformation matrix given current viewint properties
        basicModelViewModel.updateViewTransform(dc); // update view angles etc.
        
        // Set current GL viewing state.
        Matrix modelViewMatrix =  this.createModelViewMatrix(this.basicModelViewModel);
        Matrix projectionMatrix = this.createProjectionMatrix(nearDistance, farDistance);
        
        //this.loadModelViewProjection(dc, modelViewMatrix, projectionMatrix);
        viewSupport.loadGLViewState(dc, modelViewMatrix, projectionMatrix);
    }

    public Frustum getFrustum()
    {
        return this.frustum;
    }

    private Matrix createModelViewMatrix(BasicModelViewModel orbitViewModel)
    {
        if (orbitViewModel == null)
            return null;
        
        return orbitViewModel.getTransformMatrix();
    }

    private Matrix createProjectionMatrix(double nearClipDistance, double farClipDistance)
    {
        Angle fov = this.getFieldOfView();
        if (fov == null)
            return null;

        java.awt.Rectangle viewport = this.getViewport();
        if (viewport == null)
            return null;

        // Create a standard perspective projection.
        return Matrix.fromPerspective(
            fov,
            viewport.width, viewport.height,
            nearClipDistance, farClipDistance);
    }

    private Frustum createFrustum(double nearClipDistance, double farClipDistance)
    {
        Angle fov = this.getFieldOfView();
        if (fov == null)
            return null;

        java.awt.Rectangle viewport = this.getViewport();
//        if (viewport == null)
//            return null;
        
        // Create a standard perspective frustum.
        return Frustum.fromPerspective(
            fov, viewport.width, viewport.height,
            nearClipDistance, farClipDistance);
    }

    // ============== Attribute Initialization ======================= //

    protected void doInitialize(DrawContext dc)
    {
        this.basicModelViewModel = new BasicModelViewModel(dc); 
        
        setCenterPosition(iniPosition); 
    }
    
    
    // get / set  -- and other needed functions
    public Angle getFieldOfView()
    {
        return this.fieldOfView;
    }

    public void setFieldOfView(Angle fieldOfView)
    {
        if (fieldOfView == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.fieldOfView = fieldOfView;
    }

    public double getNearClipDistance()
    {
        return this.nearClipDistance;
    }

    public void setNearClipDistance(double distance)
    {
        this.nearClipDistance = distance;
    }

    public double getFarClipDistance()
    {
        return this.farClipDistance;
    }

    public void setFarClipDistance(double distance)
    {
        this.farClipDistance = distance;
    }

    public double getAutoNearClipDistance()
    {
        Position eyePos = getCurrentEyePosition();
        return computeNearDistance(eyePos);
    }

    public double getAutoFarClipDistance()
    {
        Position eyePos = getCurrentEyePosition();
        return computeFarDistance(eyePos);
    }
    
    public java.awt.Rectangle getViewport()
    {
        // java.awt.Rectangle is mutable, so we defensively copy the viewport.
        return new java.awt.Rectangle(this.viewport);
    }
    
    public Position getCurrentEyePosition()
    {
        if (this.globe != null)
        {
//            Matrix modelview = this.orbitViewModel.computeTransformMatrix(this.globe, this.center,
//                    this.heading, this.pitch, this.zoom);
//            if (modelview != null)
//            {
//                Matrix modelviewInv = modelview.getInverse();
//                if (modelviewInv != null)
//                {
//                    Vec4 eyePoint = Vec4.UNIT_W.transformBy4(modelviewInv);
//                    return this.globe.computePositionFromPoint(eyePoint);
//                }
//            }
            
            return basicModelViewModel.getEyePosition(dc);
        }

        return Position.ZERO;
    }
    
     private double computeNearDistance(Position eyePosition)
    {
        double near = 0;
        if (eyePosition != null && this.dc != null)
        {
            double elevation = this.viewSupport.computeElevationAboveSurface(this.dc, eyePosition);
            double tanHalfFov = this.fieldOfView.tanHalfAngle();
            near = elevation / (2 * Math.sqrt(2 * tanHalfFov * tanHalfFov + 1));
        }
        return near < MINIMUM_NEAR_DISTANCE ? MINIMUM_NEAR_DISTANCE : near;
    }

    private double computeFarDistance(Position eyePosition)
    {
        double far = 0;
        if (eyePosition != null)
        {
            far = computeHorizonDistance(eyePosition);
        }
        
        return far < MINIMUM_FAR_DISTANCE ? MINIMUM_FAR_DISTANCE : far;
    }
    
    private double computeHorizonDistance(Position eyePosition)
    {
        if (this.globe != null && eyePosition != null)
        {
            double elevation = eyePosition.getElevation();
            double elevationAboveSurface = this.viewSupport.computeElevationAboveSurface(this.dc, eyePosition);
            return this.viewSupport.computeHorizonDistance(this.globe, Math.max(elevation, elevationAboveSurface));
        }

        return 0;
    }
    
    public double computeHorizonDistance()
    {
        double horizon = 0;
        Position eyePos = computeEyePositionFromModelview();
        if (eyePos != null)
        {
            horizon = computeHorizonDistance(eyePos);
        }

        return horizon;
    }
    
    private Position computeEyePositionFromModelview()
    {
//        if (this.globe != null)
//        {
//            Vec4 eyePoint = Vec4.UNIT_W.transformBy4(this.modelviewInv);
//            return this.globe.computePositionFromPoint(eyePoint);
//        }

        return Position.ZERO;
    }
    
    public double computePixelSizeAtDistance(double distance)
    {
        return this.viewSupport.computePixelSizeAtDistance(distance, this.fieldOfView, this.viewport);
    }
    
    public Position computePositionFromScreenPoint(double x, double y)
    {
        if (this.globe != null)
        {
            Line ray = computeRayFromScreenPoint(x, y);
            if (ray != null)
                return this.globe.getIntersectionPosition(ray);
        }

        return null;
    }
    
     public Line computeRayFromScreenPoint(double x, double y)
    {
        return null;//this.viewSupport.computeRayFromScreenPoint(x, y, this.modelview, this.projection, this.viewport);
    }
     
     public Vec4 unProject(Vec4 windowPoint)
    {
        if (windowPoint == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return null;//this.viewSupport.unProject(windowPoint, this.modelview, this.projection, this.viewport);
    }
     
     public Vec4 project(Vec4 modelPoint)
    {
        if (modelPoint == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return null;//this.viewSupport.project(modelPoint, this.modelview, this.projection, this.viewport);
    }
     
     public Matrix getProjectionMatrix()
    {
        return null;//this.projection;

    }
     
     public Frustum getFrustumInModelCoordinates()
    {
//        if (this.lastFrustumInModelCoords == null)
//        {
//            Matrix modelviewTranspose = this.modelview.getTranspose();
//            if (modelviewTranspose != null)
//                this.lastFrustumInModelCoords = this.frustum.transformBy(modelviewTranspose);
//            else
//                this.lastFrustumInModelCoords = this.frustum;
//        }
        return null;//this.lastFrustumInModelCoords;
    }
     
     public Matrix getModelviewMatrix()
    {
        return null;//this.modelview;
    }
     
     public Vec4 getForwardVector()
    {
//        if (this.lastForwardVector == null)
//            this.lastForwardVector = Vec4.UNIT_NEGATIVE_Z.transformBy4(this.modelviewInv);
//        return this.lastForwardVector;
         return null;
    }
     
     public Vec4 getUpVector()
    {
//        if (this.lastUpVector == null)
//            this.lastUpVector = Vec4.UNIT_Y.transformBy4(this.modelviewInv);
//        return this.lastUpVector;
         return null;
    }
     
  public Vec4 getCurrentEyePoint()
    {
//        if (this.globe != null)
//        {
//            Matrix modelview = this.orbitViewModel.computeTransformMatrix(this.globe, this.center,
//                    this.heading, this.pitch, this.zoom);
//            if (modelview != null)
//            {
//                Matrix modelviewInv = modelview.getInverse();
//                if (modelviewInv != null)
//                {
//                    return Vec4.UNIT_W.transformBy4(modelviewInv);
//                }
//            }
//        }

        return Vec4.ZERO;
    }
  
  public Vec4 getEyePoint()
    {
//        if (this.lastEyePoint == null)
//            this.lastEyePoint = Vec4.UNIT_W.transformBy4(this.modelviewInv);
//        return this.lastEyePoint;
      
      return null;
    }
public void setOrientation(Position eyePosition, Position centerPosition)
    {
}
public void setEyePosition(Position eyePosition)
    {}

public Position getEyePosition()
    {
        return null;
    }

public void restoreState(String stateInXml)
    {
    }

    @Override
public String getRestorableState()
    {
    return "";
}


}

  
