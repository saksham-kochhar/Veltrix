package com.example.veltrix


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch

class veltrixviewmodel : ViewModel(){
    val messagelist by lazy {
        mutableStateListOf<Response>()
    }
    var loading by mutableStateOf(false)

    var instruction by mutableStateOf(Instruction.normal)
    val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-2.5-flash-lite" ,
            systemInstruction = content {
                text(
                    instruction
                )
            })



    fun sendmessage(question : String){
        val chat = model.startChat(
            history = messagelist.map {
                content(it.Role){text(it.message)}
            }.toList()
        )


        viewModelScope.launch {
            loading = true
            try {

                messagelist.add(Response(question, "User"))
                val response = chat.sendMessage(question)
                messagelist.add(Response(response.text.toString(), "Model"))
            }
            catch (e: Exception) {
                messagelist.add(Response("Error: ${e.message}", "Model"))
            }
            finally {
                loading = false
            }
        }
        }
}