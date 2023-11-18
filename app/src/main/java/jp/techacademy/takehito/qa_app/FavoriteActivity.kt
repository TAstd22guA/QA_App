package jp.techacademy.takehito.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.takehito.qa_app.databinding.ActivityFavoriteBinding

class FavoriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoriteBinding
    //　favorite一覧を表示するための処理

    //カリキュラム（MainActivity）で作成
    private lateinit var databaseReference: DatabaseReference
    private lateinit var favoriteArrayList: ArrayList<Question>
    private lateinit var adapter: QuestionsListAdapter

    private var genreRef: DatabaseReference? = null
    //カリキュラム（MainActivity）作成ここまで

    private var favoriteRef: DatabaseReference? = null


    //カリキュラム（MainActivity）で作成
    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            favoriteVal(dataSnapshot, s)
        }

        //カリキュラム（MainActivity）で作成
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            favoriteVal(dataSnapshot, s)
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }
    //カリキュラム（MainActivity）作成ここまで


    //カリキュラム（MainActivity）で作成　Activity_favoriteの作成
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*
        タイトルが「QA_App」になるので、タイトルをお気に入りに修正
         */
        title = getString(R.string.menu_favorite_label)

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        adapter = QuestionsListAdapter(this)
        favoriteArrayList = ArrayList()
        adapter.notifyDataSetChanged()

        binding.listView.setOnItemClickListener { _, _, position, _ ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", favoriteArrayList[position])
            startActivity(intent)
        }
    }
    //カリキュラム（MainActivity）作成ここまで

    //カリキュラム（MainActivity）で作成
    override fun onResume() {
        super.onResume()
        //ここから修正
        favoriteArrayList.clear()
        adapter.setQuestionArrayList(favoriteArrayList)
        binding.listView.adapter = adapter

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            favoriteRef = databaseReference.child(FavoritePATH).child(user.uid)
        }
        favoriteRef!!.addChildEventListener(eventListener)

    }


    /*
    課題用追加　　onChildAdded、onChildChangeの定数
    */
    private fun favoriteVal(dataSnapshot: DataSnapshot, s: String?){
        val map = dataSnapshot.value as Map<*, *>

        //ここから修正(追加)
        val questionUid = dataSnapshot.key
        val genre = map["genre"] as? String ?: ""

        genreRef =
            databaseReference.child(ContentsPATH).child(genre).child(questionUid.toString())

        genreRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(Snapshot: DataSnapshot) {
                val map = Snapshot.value as Map<*, *>
                val title = map["title"] as? String ?: ""
                val body = map["body"] as? String ?: ""
                val name = map["name"] as? String ?: ""
                val uid = map["uid"] as? String ?: ""
                val imageString = map["image"] as? String ?: ""
                val bytes =
                    if (imageString.isNotEmpty()) {
                        Base64.decode(imageString, Base64.DEFAULT)
                    } else {
                        byteArrayOf()
                    }
                //修正ここまで

                //カリキュラム（MainActivity）で作成
                val answerArrayList = ArrayList<Answer>()
                val answerMap = map["answers"] as Map<*, *>?
                if (answerMap != null) {
                    for (key in answerMap.keys) {
                        val map1 = answerMap[key] as Map<*, *>
                        val map1Body = map1["body"] as? String ?: ""
                        val map1Name = map1["name"] as? String ?: ""
                        val map1Uid = map1["uid"] as? String ?: ""
                        val map1AnswerUid = key as? String ?: ""
                        val answer = Answer(map1Body, map1Name, map1Uid, map1AnswerUid)
                        answerArrayList.add(answer)
                    }
                }

                val question = Question(
                    title, body, name, uid, dataSnapshot.key ?: "",
                    genre, bytes, answerArrayList
                )

                favoriteArrayList.add(question)
                adapter.notifyDataSetChanged()
            }
            //カリキュラム（MainActivity）作成ここまで

            //ここから修正(追加)
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}








