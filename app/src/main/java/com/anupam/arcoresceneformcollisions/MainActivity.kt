package com.anupam.arcoresceneformcollisions

import androidx.appcompat.app.AppCompatActivity

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import java.util.*


class MainActivity : AppCompatActivity(), Scene.OnUpdateListener {


    private var arFragment: ArFragment? = null
    private var tvDistance: TextView? = null
    private var cubeRenderable: ModelRenderable? = null

    private var nodeA: TransformableNode? = null
    private var nodeB: TransformableNode? = null

    var greenMaterial: Material? = null
    var originalMaterial: Material? = null

    var overlapIdle = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(applicationContext, "Device not supported", Toast.LENGTH_LONG).show()
        }

        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        tvDistance = findViewById(R.id.tvDistance)

        initModel()

        arFragment!!.setOnTapArPlaneListener { hitResult, plane, motionEvent ->

            if (cubeRenderable != null) {

                val anchor = hitResult.createAnchor()
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arFragment!!.arSceneView.scene)

                if (nodeA != null && nodeB != null) {
                    clearAnchors()
                }

                val node = TransformableNode(arFragment!!.transformationSystem)
                node.renderable = cubeRenderable
                node.setParent(anchorNode)

                arFragment!!.arSceneView.scene.addChild(anchorNode)
                node.select()

                if (nodeA == null) {
                    nodeA = node
                    arFragment!!.arSceneView.scene.addOnUpdateListener(this)
                } else if (nodeB == null) {
                    nodeB = node

                }
            }
        }
    }


    private fun initModel() {

        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.GREEN))
            .thenAccept { material ->
            greenMaterial = material
        }

        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.RED))
            .thenAccept { material ->
                val vector3 = Vector3(0.05f, 0.05f, 0.05f)
                cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material)
                originalMaterial = material

                cubeRenderable!!.isShadowCaster = false
                cubeRenderable!!.isShadowReceiver = false

            }
    }


    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {

        val openGlVersionString = (Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE)) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }

    private fun clearAnchors() {

        arFragment!!.arSceneView.scene.removeChild(nodeA!!.parent!!)
        arFragment!!.arSceneView.scene.removeChild(nodeB!!.parent!!)

        nodeA = null
        nodeB = null
    }

    override fun onUpdate(frameTime: FrameTime) {

        if (nodeA != null && nodeB != null) {

            var node = arFragment!!.arSceneView.scene.overlapTest(nodeA)

            if (node != null) {

                if (overlapIdle) {
                    overlapIdle = false
                    nodeA!!.renderable!!.material = greenMaterial
                }

            } else {

                if (!overlapIdle) {
                    overlapIdle = true
                    nodeA!!.renderable!!.material = originalMaterial
                }
            }

            val positionA = nodeA!!.worldPosition
            val positionB = nodeB!!.worldPosition

            val dx = positionA.x - positionB.x
            val dy = positionA.y - positionB.y
            val dz = positionA.z - positionB.z

            //Computing a straight-line distance.
            val distanceMeters = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()

            val distanceFormatted = String.format("%.2f", distanceMeters)

            tvDistance!!.text = "Distance between nodes: $distanceFormatted metres"


        }
    }

    companion object {
        private val MIN_OPENGL_VERSION = 3.0
    }
}



