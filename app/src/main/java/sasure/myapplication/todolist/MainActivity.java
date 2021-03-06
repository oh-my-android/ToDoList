package sasure.myapplication.todolist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;

import java.util.ArrayList;

import sasure.myapplication.listview.ContentItem;
import sasure.myapplication.listview.LabelItem;
import sasure.myapplication.listview.ListItem;
import sasure.myapplication.listview.PartAdapter;
import sasure.myapplication.listview.slidecutListView;
import sasure.myapplication.mysql.mDataBaseHelper;

public class MainActivity extends SherlockListActivity implements slidecutListView.RemoveListener ,
        ListView.OnItemClickListener,ListView.OnScrollListener,DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener
{
    /**
     * listView上划时按钮的透明度
     */
    private final float upAlpha = 1f;

    /**
     * listView下划时按钮的透明度
     */
    private final float downAlpha = 0.328f;

    /**
     * 按钮透明度变化动画时长
     */
    private final long alphaDuration = 618;

    /**
     * 通知handle
     */
    private final static int FLASH = 0x00;

    /**
     * 通知初始化listview
     */
 //   private final static int INIT = 0x01;

    /**
     * 无内容
     */
    private final static int NO_CONTENT = 0x02;

    /**
     * 有内容
     */
    private final static int HAVE_CONTENT = 0x03;

    /**
     * 未完成清单的数量
     */
    private int unFinishCount = 0;

    /**
     * 已完成清单的数量
     */
    private int FinishCount = 0;

    /**
     * 未完成清单的标题
     */
    private LabelItem unFinishLable;

    /**
     * 已完成清单的标题
     */
    private LabelItem FinishLable;

    /**
     * 内容为空时
     */
    private View noContentView;

    /**
     * 主界面的FrameLayout
     */
    private FrameLayout mainLayout;

    /**
     * SlidingMenu对象
     */
//    private SlidingMenu slidingMenu;

    /**
     * 下拉选择框对象
     */
    private Spinner mSpinner;

    /**
     * 保留包信息，并开放
     */
    public static Context mContext;

    /**
     * 加载类
     */
 //   private LayoutInflater mInflater;

    /**
     * 保存listview的每行信息
     */
    private ArrayList<ListItem> mListItems;

    /**
     * 加载ListViewActivity的ListView
     */
    private slidecutListView mListView;

    /**
     * 保存屏幕宽度
     */
    public static int screenWidth;

    /**
     * 主屏幕的添加按钮
     */
    private ImageButton buttonAdd;

    /**
     * 保存数据库信息
     */
    private SQLiteDatabase db;

    /**
     * 当前屏幕可见的第一个item在整个listview中的下标
     */
    private int firstIndex;

    /**
     * 确定删除对话框
     */
    private AlertDialog deleteDialog;

    public static final String PREFERENCE_NAME = "AndroidCommon";
    public static final String FIRSTTIME = "FirstTime";
    /**
     * 当前选中行
     */
    private int position;

    /**
     * 初始化handler对象
     */
    private final Handler handle = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case FLASH:
                    if (mListView == null)
                        initListView();
                    else
                        mListView.invalidateViews();
                    break;

                case NO_CONTENT:
                    addNoContentView();
                    break;

                case HAVE_CONTENT:
                    removeNoContentView();
                    break;

                default:
                     super.handleMessage(msg);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        initField();
        initActionbar();
//        initSlidingMenu();
        initaddButton();
        firstTime();
    }

    private void firstTime()
    {
        SharedPreferences preferences = getSharedPreferences(PREFERENCE_NAME,MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if(!preferences.getBoolean(FIRSTTIME,false))
        {
            String titleStatement = "insert into " + mDataBaseHelper.TITLE_TABLE + "(" + mDataBaseHelper.TITLE + "," +
                    mDataBaseHelper.TYPE +") values('左划删除，右划归类','" + mDataBaseHelper.STUDY + "')";

            db.execSQL(titleStatement);

            final String selectStatement = "select distinct last_insert_rowid() from " + mDataBaseHelper.TITLE_TABLE;

            Cursor cursor = db.rawQuery(selectStatement,null);
            int title_id = 0;

            if(cursor != null)
                if(cursor.moveToFirst())
                    title_id = cursor.getInt(0);

            String contentStatement = "insert into " + mDataBaseHelper.DETAIL_TABLE + "(" + mDataBaseHelper.DETAIL +","+mDataBaseHelper.TITLE_ID+
                    ") values('长按编辑,感谢您的支持！！',"+ title_id +")";

            db.execSQL(contentStatement);

            editor.putBoolean(FIRSTTIME,true);
            editor.commit();
        }

     //   db.execSQL("insert into title_table(title,type) values('2','study')");
    }

    @Override
    public void onResume()
    {
        super.onResume();

        initListItems();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(mDataBaseHelper.getInstance() != null)
            mDataBaseHelper.getInstance().close();
    }

    /**
     * 初始化各种对象
     */
    private void initField()
    {
        mContext = this;
        //mInflater = LayoutInflater.from(mContext);
        mListItems = new ArrayList<>();
        screenWidth = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();//一定要在ListView之前调用！！
        db = mDataBaseHelper.getInstance().getReadableDatabase();
        unFinishLable = new LabelItem(getResources().getString(R.string.unfinished));
        FinishLable = new LabelItem(getResources().getString(R.string.finished));

        View tp = View.inflate(this,R.layout.dialog,null);
        TextView tv = (TextView) tp.findViewById(R.id.dialog_textview);
        tv.setText(getResources().getString(R.string.to_delete));
        deleteDialog = createDialog(tp).setPositiveButton(getResources().getString(R.string.sure),this).setOnCancelListener(this)
                .create();

        noContentView = View.inflate(this,R.layout.no_content,null);
        mainLayout = (FrameLayout) View.inflate(this,R.layout.activity_main,null);
        setContentView(mainLayout);
        db.execSQL("PRAGMA foreign_keys=ON");//开启sqlite的外键
    }

    /**
     *添加无清单提示
     */
    private void addNoContentView()
    {
        if(mainLayout.findViewById(R.id.no_content) == null)
            mainLayout.addView(noContentView);

      //  mainLayout.invalidate();
    }

    /**
     * 移除无清单提示
     */
    private void removeNoContentView()
    {
        if(mainLayout.findViewById(R.id.no_content) != null)
            mainLayout.removeView(noContentView);

     //   mainLayout.invalidate();
    }

    /**
     * 初始化ActionBar
     */
    private void initActionbar()
    {
        ActionBar actionBar = getSupportActionBar();
    //    actionBar.setDisplayHomeAsUpEnabled(false);
     //   actionBar.setDisplayShowHomeEnabled(false);

        ArrayAdapter<CharSequence> list =  ArrayAdapter.createFromResource(this, R.array.type, R.layout.spinner_item);
        list.setDropDownViewResource(R.layout.spinner_dropdown_item);

        View myView = LayoutInflater.from(this).inflate(R.layout.myspinner, null);
        mSpinner = (Spinner) myView.findViewById(R.id.myspinner);
        mSpinner.setAdapter(list);

        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.RIGHT; // set your layout's gravity to 'right'
        actionBar.setCustomView(myView, layoutParams);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                mSpinner.setSelection(position);
                initListItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
    }

    /**
     * 刷新列表信息
     */
    private void initListItems()
    {
 //       ArrayList<ContentItem> unFinished = new ArrayList<>();
  //      ArrayList<ContentItem> Finished = new ArrayList<>();
//        hasFinished = false;
//        hasUnFinished = false;

//        setSupportProgress(Window.PROGRESS_START);
//        setSupportProgressBarVisibility(true);

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                unFinishCount = 0;
                FinishCount = 0;

                Cursor cursor = db.rawQuery(getQueryStatement(),null);

                //  cursor.moveToLast();

                mListItems.clear();

                if(cursor != null)
                    while (cursor.moveToNext())
                    {
                        int _id = cursor.getInt(cursor.getColumnIndex(mDataBaseHelper._ID));
                        String title = cursor.getString(cursor.getColumnIndex(mDataBaseHelper.TITLE));
                        String type = cursor.getString(cursor.getColumnIndex(mDataBaseHelper.TYPE));
                        String isDone = cursor.getString(cursor.getColumnIndex(mDataBaseHelper.ISDONE));

                        ContentItem tp = new ContentItem(_id,title,type,isDone);

                        switch (isDone)
                        {
                            case mDataBaseHelper.FALSE:
                                //   unFinished.add(tp);
                                if(unFinishCount == 0)
                                    mListItems.add(0,unFinishLable);

                                ++unFinishCount;

                                mListItems.add(unFinishCount,tp);

                                break;

                            case mDataBaseHelper.TRUE:
                                //Finished.add(tp);
                                int f = unFinishCount == 0 ? 0 : unFinishCount + 1;

                                if(FinishCount == 0)
                                    mListItems.add(f,FinishLable);

                                ++FinishCount;

                                mListItems.add(f + FinishCount,tp);
                                break;

                            default:
                                Log.i("warn","isDone just can true or false");
                                break;
                        }
                    }

                if(!mListItems.isEmpty())
                {
                    handle.sendEmptyMessage(HAVE_CONTENT);
                }
                else
                    handle.sendEmptyMessage(NO_CONTENT);

                handle.sendEmptyMessage(FLASH);
            }
        }).start();
    }

    /**
     * 根据下拉选择框选项的不同生成不同的查询语句
     * @return
     */
    private String getQueryStatement()
    {
        StringBuffer statement = new StringBuffer();

        statement.append("select * from " + mDataBaseHelper.TITLE_TABLE);

        switch (mSpinner.getSelectedItemPosition())
        {
            case 0:
                break;

            case 1:
                statement.append(" where " + mDataBaseHelper.TYPE + " = '" + mDataBaseHelper.LIFE + "'");
                break;

            case 2:
                statement.append(" where " + mDataBaseHelper.TYPE + " = '" + mDataBaseHelper.STUDY + "'");
                break;

            case 3:
                statement.append(" where " + mDataBaseHelper.TYPE + " = '" + mDataBaseHelper.WORK + "'");
                break;

            default:
                break;
        }

        statement.append(" order by " + mDataBaseHelper.LOGTIME + " desc");

        Log.i("test",statement.toString());

        return statement.toString();
    }

    /**
     * 初始化SlidingMenu
     */
//    private void initSlidingMenu()
//    {
//        setContentView(mainLayout);
//        setBehindContentView(R.layout.hidingview);
//
//     //   mainLayout.addView(noContentView);
//        slidingMenu = getSlidingMenu();
//        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
//        slidingMenu.setFadeEnabled(false);
//        slidingMenu.setFadeDegree(0.618f);
//        slidingMenu.setShadowDrawable(R.drawable.shadow);
//        slidingMenu.setShadowWidthRes(R.dimen.slide_shadow_width);
//        slidingMenu.setBehindWidthRes(R.dimen.behind_width);
//    }

    /**
     * 初始化ListView
     */
    public void initListView()
    {
        mListView = (slidecutListView) getListView();

//        String[] Countries = getResources().getStringArray(R.array.countries);
//        LabelItem label = new LabelItem("近期");
//        mListItems.add(label);

//        for (int i = 0; i < 30; i++)
//        {
//                String item = new String("" + Countries[i]);
//                ContentItem content = new ContentItem(item);
//                mListItems.add(content);
//        }

   //     initListItems();

     //   listAdapter = new PartAdapter(mListItems, mContext);

    //    mListView.setLayoutAnimation(getListAnim());
        mListView.setAdapter(new PartAdapter(mListItems, mContext));
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);
        mListView.setRemoveListener(this);
    }

    /**
     * 加载添加按钮
     */
    public void initaddButton()
    {
        buttonAdd = (ImageButton) findViewById(R.id.button_add);

        buttonAdd.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                buttonAdd.setAlpha(1f);
                Intent tp = new Intent(MainActivity.this,EditActivity.class);

                startActivity(tp);
            }
        });

        buttonAdd.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
              //  dd();
                initListItems();
                return true;
            }
        });
    }

    /**
     *  加载动画
     * @return 飞入效果
     */
//    public LayoutAnimationController getAnim()
//    {
//        Animation layoutAnim = AnimationUtils.loadAnimation(this, R.anim.layout_anim);
//  //      layoutAnim.setDuration(550);
//
//        LayoutAnimationController controller = new LayoutAnimationController(layoutAnim);
//        return controller;
//    }

//    @Override
//    public void onBackPressed()
//    {
//        if (slidingMenu.isMenuShowing())
//            slidingMenu.showContent();
//        else
//            super.onBackPressed();
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item)
//    {
//        switch (item.getItemId())
//        {
//            case android.R.id.home:
//                toggle();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    /**
     * 菜单、返回键响应
     */
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event)
//    {
//        if(keyCode == KeyEvent.KEYCODE_MENU)
//        {
//            toggle();
//            return true;
//        }
//
//        return super.onKeyDown(keyCode,event);
//    }

    @Override
    public void removeItem(slidecutListView.RemoveDirection direction, int position)
    {
        this.position = position;

        switch (direction)
        {
            case RIGHT:
                toDone();
                break;
            case LEFT:
               // toDelete();
                deleteDialog.show();
                break;

//            case BACK:
//                Toast.makeText(this,"Back",Toast.LENGTH_SHORT).show();
//                break;

            default:
                break;
        }


    }

    /**
     * 创建对话框
     */
    private AlertDialog.Builder createDialog(View view)
    {
        return new AlertDialog.Builder(this).setView(view);
    }

    /**
     * 右滑完成触发
     */
    private void toDone()
    {
        final String toFinish = "update " + mDataBaseHelper.TITLE_TABLE + " set " + mDataBaseHelper.ISDONE +
                " = '" + mDataBaseHelper.TRUE + "'," + mDataBaseHelper.LOGTIME +" = " + mDataBaseHelper.CURRENTTIME +
                " where " + mDataBaseHelper._ID + " = ?";

        final String toUnFinish = "update " + mDataBaseHelper.TITLE_TABLE + " set " + mDataBaseHelper.ISDONE +
                " = '" + mDataBaseHelper.FALSE + "'," + mDataBaseHelper.LOGTIME +" = " + mDataBaseHelper.CURRENTTIME +
                " where " + mDataBaseHelper._ID + " = ?";

        final ContentItem tp = (ContentItem) mListItems.get(position);

        if(tp.isDone.equals(mDataBaseHelper.FALSE))//从未完成到已完成
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    db.execSQL(toFinish,new String[]{tp._id+""});
                }
            }).start();

            if(FinishCount == 0)
                mListItems.add(FinishLable);

            ++FinishCount;
            --unFinishCount;

            removeLable();

            tp.isDone = mDataBaseHelper.TRUE;
            mListItems.remove(tp);
            mListItems.add((unFinishCount == 0 ? 1 : unFinishCount + 2),tp);

            Toast.makeText(this,getResources().getString(R.string.toast_unf_to_f), Toast.LENGTH_SHORT).show();
        }
        else if(tp.isDone.equals(mDataBaseHelper.TRUE))//从已完成到未完成
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    db.execSQL(toUnFinish,new String[]{tp._id+""});
                }
            }).start();

            if(unFinishCount == 0)
                mListItems.add(0,unFinishLable);

            ++unFinishCount;
            --FinishCount;

            removeLable();

            tp.isDone = mDataBaseHelper.FALSE;
            mListItems.remove(tp);
            mListItems.add(1,tp);

            Toast.makeText(this,getResources().getString(R.string.toast_f_to_unf), Toast.LENGTH_SHORT).show();
        }

        handle.sendEmptyMessage(FLASH);
      //  changeElse(position);
   //     initListItems();
     //   listAdapter.notifyDataSetInvalidated();
    }

    /**
     * 左滑删除触发
     */
    private void toDelete()
    {
        final String statement = "delete from " + mDataBaseHelper.TITLE_TABLE + " where " + mDataBaseHelper._ID +" = ?";
        final ContentItem tp = (ContentItem) mListItems.get(position);

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                db.execSQL(statement,new String[]{tp._id+""});
            }
        }).start();

        if(tp.isDone.equals(mDataBaseHelper.FALSE))
            --unFinishCount;
        else if(tp.isDone.equals(mDataBaseHelper.TRUE))
            --FinishCount;

        mListItems.remove(tp);
        removeLable();

        if(mListItems.isEmpty())
            handle.sendEmptyMessage(NO_CONTENT);

        handle.sendEmptyMessage(FLASH);
        Toast.makeText(this,getResources().getString(R.string.delete), Toast.LENGTH_SHORT).show();
    }

    /**
     * 移除标题
     */
    private void removeLable()
    {
        if(unFinishCount == 0)
            mListItems.remove(unFinishLable);

        if(FinishCount == 0)
            mListItems.remove(FinishLable);

    }

//    private void changeElse(int position)
//    {
//        if (listAdapter.getCount() != position)
//        {
//            if (listAdapter.getItem(position).getClass() == LabelItem.class && listAdapter.getItem(position - 1).getClass() == LabelItem.class)
//            {
//                hasUnFinished = false;
//                mListItems.remove(position - 1);
//            }
//        }
//        else
//        {
//            if (listAdapter.getItem(position - 1).getClass() == LabelItem.class)
//            {
//                if(hasUnFinished == true)
//                    hasUnFinished = false;
//                else
//                    hasFinished = false;
//
//                mListItems.remove(position - 1);
//            }
//        }
//    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
//        mAdapter.setSelectedPosition(position);
//        mAdapter.notifyDataSetInvalidated();
        Intent intent = new Intent(this,EditActivity.class);
        Bundle bundle = new Bundle();

        ContentItem item;

        if(mListItems.get(position).getClass() == ContentItem.class)
        {
            item = (ContentItem) mListItems.get(position);

            bundle.putInt(mDataBaseHelper.TITLE_ID, item._id);
            bundle.putString(mDataBaseHelper.TITLE, item.mTitle);
            bundle.putString(mDataBaseHelper.TYPE, item.mType);
            bundle.putString(mDataBaseHelper.ISDONE, item.isDone);
            //   bundle.putString(mDataBaseHelper.ISDONE,item.isDone);

            intent.putExtra(EditActivity.BUNDLE_CONTENT, bundle);

            startActivity(intent);
        }
        //Toast.makeText(MainActivity.this,position+"",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState)
    {
        switch (scrollState)
        {
            case SCROLL_STATE_TOUCH_SCROLL:
                firstIndex = view.getLastVisiblePosition();
                break;

            case SCROLL_STATE_FLING:
                int nextIndex = view.getLastVisiblePosition();

                if(nextIndex > firstIndex)//向下滚动
                {
                    if(buttonAdd.getAlpha() == upAlpha)
                    {
                        ObjectAnimator tp1 = ObjectAnimator.ofFloat(buttonAdd, "alpha", downAlpha).setDuration(alphaDuration);
                        tp1.setInterpolator(new AccelerateDecelerateInterpolator());

                        tp1.start();
                    }
                }
                else //向上滚动
                {
                    if(buttonAdd.getAlpha() == downAlpha)
                    {
                        ObjectAnimator tp2 = ObjectAnimator.ofFloat(buttonAdd, "alpha", upAlpha).setDuration(alphaDuration);
                        tp2.setInterpolator(new AccelerateDecelerateInterpolator());

                        tp2.start();
                    }
                }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
    {}

    @Override
    public void onClick(DialogInterface dialog, int which)
    {
        toDelete();
        deleteDialog.dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        mListView.scrollBack();
    }
}