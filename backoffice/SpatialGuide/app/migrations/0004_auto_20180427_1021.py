# Generated by Django 2.0.3 on 2018-04-27 10:21

import datetime
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0003_auto_20180426_1549'),
    ]

    operations = [
        migrations.AlterField(
            model_name='point',
            name='LastUpdate',
            field=models.BigIntegerField(default=1524824471762),
        ),
        migrations.AlterField(
            model_name='point',
            name='Point_Date',
            field=models.DateField(default=datetime.date(2018, 4, 27)),
        ),
        migrations.AlterField(
            model_name='route',
            name='LastUpdate',
            field=models.BigIntegerField(default=1524824471761),
        ),
        migrations.AlterField(
            model_name='route',
            name='Route_Date',
            field=models.DateField(default=datetime.date(2018, 4, 27)),
        ),
    ]
