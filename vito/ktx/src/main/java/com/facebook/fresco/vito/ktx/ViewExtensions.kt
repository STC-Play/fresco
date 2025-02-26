/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.ktx

import android.net.Uri
import android.view.View
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.view.VitoView

object ViewExtensions {

  fun View.show(uri: Uri) = VitoView.show(uri, this)

  fun View.show(uri: Uri, callerContext: Any) = VitoView.show(uri, callerContext, this)

  fun View.show(uri: Uri, imageOptions: ImageOptions) = VitoView.show(uri, imageOptions, this)

  fun View.show(
      uri: Uri,
      imageOptions: ImageOptions,
      imageListener: ImageListener,
      callerContext: Any? = null
  ) = VitoView.show(uri, imageOptions, callerContext, imageListener, this)

  fun View.show(uri: Uri, imageOptions: ImageOptions, callerContext: Any) =
      VitoView.show(uri, imageOptions, callerContext, this)
}
