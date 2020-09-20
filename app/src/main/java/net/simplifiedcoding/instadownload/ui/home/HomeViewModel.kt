package net.simplifiedcoding.instadownload.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.simplifiedcoding.instadownload.network.InstaRepository
import net.simplifiedcoding.instadownload.network.Resource
import okhttp3.ResponseBody

class HomeViewModel(
    private val repository: InstaRepository
) : ViewModel() {

    private val _instaInfo = MutableLiveData<Resource<ResponseBody>>()
    val instaInfo: LiveData<Resource<ResponseBody>>
        get() = _instaInfo

    fun getInstaInfo(url: String) = viewModelScope.launch {
        _instaInfo.value = repository.getInstaInfo(url)
    }
}