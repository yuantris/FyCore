@file:Suppress("UNCHECKED_CAST")

package io.core.common.util.extensions.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.ComponentActivity
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import io.core.common.helper.ViewBindingProperty
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

inline fun <T : ViewBinding> androidx.core.app.ComponentActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T,
    setContentView: Boolean = false
) = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val binding = bindingInflater.invoke(layoutInflater)
    if (setContentView) {
        setContentView(binding.root)
    }
    binding
}

private class FragmentViewBindingProperty<F : Fragment, T : ViewBinding>(
    viewBinder: (F) -> T
) : ViewBindingProperty<F, T>(viewBinder) {

    override fun getLifecycleOwner(thisRef: F) = thisRef.viewLifecycleOwner
}

/**
 * Create new [ViewBinding] associated with the [Fragment]
 */
@JvmName("viewBindingFragment")
fun <F : Fragment, T : ViewBinding> Fragment.viewBinding(viewBinder: (F) -> T): ViewBindingProperty<F, T> {
    return FragmentViewBindingProperty(viewBinder)
}

/**
 * Create new [ViewBinding] associated with the [Fragment]
 *
 * @param vbFactory Function that create new instance of [ViewBinding]. `MyViewBinding::bind` can be used
 * @param viewProvider Provide a [View] from the Fragment. By default call [Fragment.requireView]
 */
@JvmName("viewBindingFragment")
inline fun <F : Fragment, T : ViewBinding> Fragment.viewBinding(
    crossinline vbFactory: (View) -> T,
    crossinline viewProvider: (F) -> View = Fragment::requireView
): ViewBindingProperty<F, T> {
    return viewBinding { fragment: F -> vbFactory(viewProvider(fragment)) }
}


/**
 * 为Fragment创建一个ViewBinding属性
 * @param T ViewBinding的类型，它决定了返回的ViewBinding实例的类型
 * @param vbFactory 一个工厂方法，用于创建ViewBinding实例
 * @param viewBindingRootId 视图绑定的根视图的ID，用于在Fragment的视图中找到根视图
 * @return 返回一个ViewBindingProperty实例，它是一个代理对象，用于管理ViewBinding实例的生命周期和访问
 *
 * 注意：这个函数使用了inline修饰符，以避免额外的类生成，保持性能
 *       它还使用了@JvmName注解，以自定义生成的字节码中的函数名称，避免名称冲突
 */
@JvmName("viewBindingFragment")
inline fun <T : ViewBinding> Fragment.viewBinding(
    crossinline vbFactory: (View) -> T,
    @IdRes viewBindingRootId: Int
): ViewBindingProperty<Fragment, T> {
    return viewBinding(vbFactory) { fragment: Fragment ->
        fragment.requireView().findViewById(viewBindingRootId)
    }
}


@JvmName("inflateWithGeneric")
fun <VB : ViewBinding> AppCompatActivity.inflateBindingWithGeneric(layoutInflater: LayoutInflater): VB =
    withGenericBindingClass(this) { clazz ->
        clazz.getMethod("inflate", LayoutInflater::class.java).invoke(null, layoutInflater) as VB
    }.also { binding ->
        if (binding is ViewDataBinding) {
            binding.lifecycleOwner = this
        }
    }

@JvmName("inflateWithGeneric")
fun <VB : ViewBinding> Fragment.inflateBindingWithGeneric(
    layoutInflater: LayoutInflater,
    parent: ViewGroup?,
    attachToParent: Boolean
): VB =
    withGenericBindingClass(this) { clazz ->
        clazz.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        ).invoke(null, layoutInflater, parent, attachToParent) as VB
    }.also { binding ->
        if (binding is ViewDataBinding) {
            binding.lifecycleOwner = viewLifecycleOwner
        }
    }

@JvmName("inflateWithGeneric")
fun <VB : ViewBinding> inflateWithGeneric(genericOwner: Any, layoutInflater: LayoutInflater): VB =
    withGenericBindingClass(genericOwner) { clazz ->
        //反射
        clazz.getMethod("inflate", LayoutInflater::class.java).invoke(null, layoutInflater) as VB
    }.also { binding ->
        if (genericOwner is ComponentActivity && binding is ViewDataBinding) {
            binding.lifecycleOwner = genericOwner
        }
    }


private fun <VB : ViewBinding> withGenericBindingClass(any: Any, block: (Class<VB>) -> VB): VB {
    var genericSuperclass = any.javaClass.genericSuperclass
    var superclass = any.javaClass.superclass
    while (superclass != null) {
        if (genericSuperclass is ParameterizedType) {
            try {
                return block.invoke(genericSuperclass.actualTypeArguments[0] as Class<VB>)
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: ClassCastException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
        genericSuperclass = superclass.genericSuperclass
        superclass = superclass.superclass
    }
    throw IllegalArgumentException("There is no generic of ViewBinding.")
}


/*******************************使用反射-popupWindow*******************************/
//Activity
inline fun <reified VB : ViewBinding> Activity.popupWindow(
    width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    focusable: Boolean = false,
    crossinline block: VB.() -> Unit
) =
    lazy {
        PopupWindow(
            inflateBinding<VB>(layoutInflater).apply(block).root,
            width,
            height,
            focusable
        )
    }

inline fun <reified VB : ViewBinding> inflateBinding(layoutInflater: LayoutInflater) =
    VB::class.java.getMethod("inflate", LayoutInflater::class.java)
        .invoke(null, layoutInflater) as VB

/**
 * 简化ViewBinding初始化
 * @param setContentView 是否自动设置contentView（默认为false）
 */
inline fun <reified VB : ViewBinding> ComponentActivity.viewBinding(
    setContentView: Boolean = false
) = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    (VB::class.java.getMethod("inflate", LayoutInflater::class.java)
        .invoke(null, layoutInflater) as VB)
        .also { if (setContentView) setContentView(it.root) }
}


//Fragment
inline fun <reified VB : ViewBinding> Fragment.popupWindow(
    width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    focusable: Boolean = false,
    crossinline block: VB.() -> Unit
) =
    lazy {
        PopupWindow(
            inflateBinding<VB>(layoutInflater).apply(block).root,
            width,
            height,
            focusable
        )
    }


/*******************************不使用反射-popupWindow*******************************/

fun <VB : ViewBinding> Activity.popupWindow(
    inflate: (LayoutInflater) -> VB,
    width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    focusable: Boolean = false,
    block: VB.() -> Unit
) = lazy {
    PopupWindow(inflate(layoutInflater).apply(block).root, width, height, focusable)
}

fun <VB : ViewBinding> Fragment.popupWindow(
    inflate: (LayoutInflater) -> VB,
    width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    focusable: Boolean = false,
    block: VB.() -> Unit
) = lazy {
    PopupWindow(inflate(layoutInflater).apply(block).root, width, height, focusable)
}
