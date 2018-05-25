package com.arashdalir.calendaralarm;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

public class AlarmListActivity extends AppCompatActivity implements AlarmsTouchHelper.AlarmTouchHelperListener {
    private RecyclerView rv;
    AlarmListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlarmManagerService.startService(this);


        setContentView(R.layout.activity_alarm_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rv = findViewById(R.id.AlarmList);

        Alarms alarms = new Alarms(getApplicationContext());

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        adapter = new AlarmListAdapter(alarms);
        rv.setLayoutManager(layoutManager);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        rv.setAdapter(adapter);

        alarms.getStoredAlarms();
        adapter.notifyDataSetChanged();

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new AlarmsTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rv);

    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof AlarmListAdapter.ViewHolder) {
            // get the removed item name to display it in snack bar
            final int deletedIndex = viewHolder.getAdapterPosition();
            String name = adapter.getItem(deletedIndex).getTitle();

            // backup of removed item for undo purpose
            final Alarms.Alarm deletedItem = adapter.getItem(deletedIndex);

            // remove the item from recycler view
            adapter.removeItem(deletedIndex);

            // showing snack bar with Undo option
            Notifier.snackBarAction action = new Notifier.snackBarAction();
            action.message = "";
            action.onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // undo is selected, restore the deleted item
                    adapter.restoreItem(deletedItem, deletedIndex);
                }
            };
            action.actionTextColor = Color.YELLOW;

            Notifier.snackBarAction[] actions = new Notifier.snackBarAction[1];
            actions[0] = action;
            Notifier.showSnackBar(rv, getString(R.string.notification_snackbar_alarm_removed, name), Snackbar.LENGTH_LONG, actions);
        }
    }
}
