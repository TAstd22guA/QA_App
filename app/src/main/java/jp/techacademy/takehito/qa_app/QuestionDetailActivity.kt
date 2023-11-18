package jp.techacademy.takehito.qa_app

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

import jp.techacademy.takehito.qa_app.databinding.ActivityQuestionDetailBinding

class QuestionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionDetailBinding

    private lateinit var question: Question
    private lateinit var adapter: QuestionDetailListAdapter
    private lateinit var answerRef: DatabaseReference

    /*
    課題用追加　定数定義
     */
    private lateinit var favoriteRef: DatabaseReference
    private var userId: String = ""
    var favoriteStar = 0

    /*
    カリキュラムのユーザー取得位置を変更
     */
    val user = FirebaseAuth.getInstance().currentUser


    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in question.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            question.answers.add(answer)
            adapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 渡ってきたQuestionのオブジェクトを保持する
        // API33以上でgetSerializableExtra(key)が非推奨となったため処理を分岐
        @Suppress("UNCHECKED_CAST", "DEPRECATION", "DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL")
        question = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getSerializableExtra("question", Question::class.java)!!
        else
            intent.getSerializableExtra("question") as? Question!!

        title = question.title

        // ListViewの準備
        adapter = QuestionDetailListAdapter(this, question)
        binding.listView.adapter = adapter
        adapter.notifyDataSetChanged()

        binding.fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            //     val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", question)
                startActivity(intent)
                // --- ここまで ---
            }
        }


        /*
         課題用追加　「お気に入りの登録」文字の表示・非表示
          */
        binding.textView3.apply {
            if (user == null) visibility = View.GONE else visibility = View.VISIBLE
        }

        /*
        課題用追加　お気に入りのボタン更新
         */
        binding.favoriteImageView.apply {

            // ログイン済みのユーザーを取得する
            //       val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                userId = FirebaseAuth.getInstance().currentUser!!.uid
            }

            if (user == null) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                val dataBaseReference = FirebaseDatabase.getInstance().reference
                //val favoriteRef = dataBaseReference.child(FavoritePATH).child(userId).child(question.questionUid)

                // お気に入りボタンが押された場合
                setOnClickListener {
                    favoriteButton(dataBaseReference)
                }
            }
        }

        Log.d("処理経過----------------------------------------", "呼び出し処理")

        val dataBaseReference = FirebaseDatabase.getInstance().reference

        /*
        課題用追加　お気に入りボタンの初期状態設定
         */
        favoriteButton(dataBaseReference)

        answerRef = dataBaseReference.child(ContentsPATH).child(question.genre.toString())
            .child(question.questionUid).child(AnswersPATH)
        answerRef.addChildEventListener(eventListener)
    }


    /*
    お気に入りボタンの処理（初期設定とボタン更新）　when 0:詳細画面に遷移したとき　1:遷移後のボタン更新
    お気に入りボタンに合わせて表示文字列と文字の色を変更　登録済み：「お気に入りの解除」（色はグレー）、未登録：「お気に入りの登録」（色は黒）
     */
    private fun favoriteButton(databaseReference: DatabaseReference) {
        favoriteRef =
            databaseReference.child(FavoritePATH).child(userId).child(question.questionUid)

        favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as Map<*, *>?
                if (data == null) {
                    when (favoriteStar) {
                        0 -> {
                            binding.favoriteImageView.setImageResource(R.drawable.ic_star_border)
                            binding.textView3.text = getString(R.string.is_star_text)
                            val color = ContextCompat.getColor(applicationContext, android.R.color.black)
                            binding.textView3.setTextColor(color)
                     //       binding.textView3.textSize = 16f
                            favoriteStar = 1
                        }
                        1 -> {
                            val data = HashMap<String, String>()
                            data["genre"] = question.genre.toString()
                            favoriteRef.setValue(data)
                            binding.favoriteImageView.setImageResource(R.drawable.ic_star)
                            binding.textView3.text = getString(R.string.is_star_border_text)
                            val color = ContextCompat.getColor(applicationContext, android.R.color.darker_gray)
                            binding.textView3.setTextColor(color)
                        }
                    }
                } else {
                    when (favoriteStar) {
                        0 -> {
                            binding.favoriteImageView.setImageResource(R.drawable.ic_star)
                            binding.textView3.text = getString(R.string.is_star_border_text)
                            val color = ContextCompat.getColor(applicationContext, android.R.color.darker_gray)
                            binding.textView3.setTextColor(color)
                            favoriteStar = 1
                        }
                        1 -> {
                            favoriteRef.removeValue()
                            binding.favoriteImageView.setImageResource(R.drawable.ic_star_border)
                            binding.textView3.text = getString(R.string.is_star_text)
                            val color = ContextCompat.getColor(applicationContext, android.R.color.black)
                            binding.textView3.setTextColor(color)
                        }
                    }
                }
            }

            override fun onCancelled(firebaseError: DatabaseError) {}
        })
    }
}
