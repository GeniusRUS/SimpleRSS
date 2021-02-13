package com.genius.srss.ui.add.folder

import com.genius.srss.di.services.database.dao.SubscriptionsDao
import moxy.MvpPresenter
import javax.inject.Inject

class AddFolderPresenter @Inject constructor(
    private val subscriptionsDao: SubscriptionsDao
) : MvpPresenter<AddFolderView>() {


}