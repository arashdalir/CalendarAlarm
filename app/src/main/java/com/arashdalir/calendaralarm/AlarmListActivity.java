package com.arashdalir.calendaralarm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.Observable;
import java.util.Observer;

public class AlarmListActivity extends AppCompatActivity implements AlarmsTouchHelper.AlarmTouchHelperListener {
    private RecyclerView rv;
    SwipeRefreshLayout refresher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_list);

        CalendarApplication application = (CalendarApplication) getApplication();
        final AlarmListAdapter adapter = application.getAdapter(this);

        refresher = findViewById(R.id.rv_alarm_list_refresher);

        refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Intent intent = new Intent(getApplicationContext(), AlarmManagerService.class);
                intent.setAction(ServiceHelper.ACTION_SET_WAKEUP_TIMERS);
                AlarmManagerService.enqueueWork(getApplicationContext(), intent);
                drawView(adapter);
                refresher.setRefreshing(false);
            }
        });

        drawView(adapter);
        observeService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void observeService(){
        ServiceHelper.observeAdapter().addObserver(new Observer() {
            @Override
            public void update(Observable o, final Object arg) {
                new Thread(){
                    public void run(){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlarmListAdapter adapter = (AlarmListAdapter) arg;
                                adapter.checkTimes();
                                drawView(adapter);
                            }
                        });
                    }
                };
            }
        });
    }

    void drawView(AlarmListAdapter adapter) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rv = findViewById(R.id.alarm_list);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        rv.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new AlarmsTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rv);
    }

    @Override
    protected void onResume() {
        CalendarApplication application = (CalendarApplication) getApplication();
        final AlarmListAdapter adapter = application.getAdapter(this);

        drawView(adapter);

        super.onResume();
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder
            viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View delete = viewHolder.itemView.findViewById(R.id.delete_bg);
            View edit = viewHolder.itemView.findViewById(R.id.edit_bg);

            if (dX < 0) {
                delete.setVisibility(View.VISIBLE);
                edit.setVisibility(View.INVISIBLE);

            } else {
                delete.setVisibility(View.INVISIBLE);
                edit.setVisibility(View.VISIBLE);
            }

        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof AlarmListAdapter.ViewHolder) {
            final AlarmListAdapter adapter = ((CalendarApplication) getApplication()).getAdapter(this);
            // get the removed item name to display it in snack bar
            final int deletedIndex = viewHolder.getAdapterPosition();
            final String name = adapter.getItem(deletedIndex).getTitle();

            // backup of removed item for undo purpose
            final Alarms.Alarm deletedItem = adapter.getItem(deletedIndex);
            final Context context = this.getApplicationContext();
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            dialog
                    .setTitle(R.string.activity_alarm_list_remove_reminder)
                    .setMessage(R.string.activity_alarm_list_remove_reminder_description)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(
                            android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // remove the item from recycler view
                                    adapter.removeItem(deletedIndex);

                                    // showing snack bar with Undo option
                                    Notifier.snackBarAction action = new Notifier.snackBarAction();
                                    action.message = "";
                                    action.onClickListener = new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {

                                            // undo is selected, restore the deleted item
                                            adapter.restoreItem(deletedItem);
                                        }
                                    };
                                    action.actionTextColor = Color.YELLOW;

                                    Notifier.snackBarAction[] actions = new Notifier.snackBarAction[1];
                                    actions[0] = action;
                                    Notifier.showSnackBar(
                                            rv,
                                            getString(R.string.notification_snackbar_alarm_removed, name),
                                            Snackbar.LENGTH_LONG,
                                            actions
                                    );
                                }
                            })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // remove the item from recycler view
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return OptionsItemSelectionHelper.handleOptionSelection(this, item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return OptionsItemSelectionHelper.createMenuItems(this, menu);
    }
}
