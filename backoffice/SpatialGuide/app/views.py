from django.shortcuts import render
from django.http import HttpRequest
from django.http import HttpResponse
from datetime import datetime
from .utils import *
from .models import *

def show_routes(request):
    assert isinstance(request, HttpRequest)

    tab_names,route_list = get_allRoutes()

    tparams = {
        'title': 'Route',
        'tab_names': tab_names,
        'route_list': route_list,
        'add_btn': 'add_route',

    }

    return render(request, 'tables.html', tparams)

def add_route(request):
    assert isinstance(request, HttpRequest)

    if request.method == 'POST':
        # create a form instance and populate it with data from the request:
        form = RouteForm(request.POST)
        if form.is_valid():
            print('Is Valid')

            name = request.POST.get('Name','')
            description= request.POST.get('Description','')

            r = Route(Name=name, Description=description)
            r.save()

        return show_routes(request)


    tparams = {
        'type': 'Route',
        'post_target': 'add_route',
        'form_t': RouteForm()
    }

    return render(request, 'form.html', tparams)

def show_points(request):
    assert isinstance(request, HttpRequest)

    tab_names,point_list = get_allPoints()

    tparams = {
        'title': 'Point',
        'tab_names': tab_names,
        'route_list': point_list,
        'add_btn': 'add_point'

    }

    return render(request, 'tables.html', tparams)

def add_point(request):
    assert isinstance(request, HttpRequest)

    tparams = {
        'type': 'Point',
        'field_names': ['Name','Latitude','Longitude'],
        'add_btn': 'show_points'
    }

    return render(request, 'form.html', tparams)




# Create your views here.
def home(request):
    """Renders the home page."""
    assert isinstance(request, HttpRequest)
    tparams = {
        'title':'Home Page',
        'year':datetime.now().year,
    }
    return render(request, 'index.html', tparams)

def tables(request):
    """Renders the contact page."""
    assert isinstance(request, HttpRequest)

    return render(request, 'tables.html', {})

def charts(request):
    """Renders the contact page."""
    assert isinstance(request, HttpRequest)

    return render(request, 'charts.html', {})

def cards(request):
    """Renders the contact page."""
    assert isinstance(request, HttpRequest)

    return render(request, 'cards.html', {})

def register(request):
    """Renders the contact page."""
    assert isinstance(request, HttpRequest)

    return render(request, 'register.html', {})


def contact(request):
    """Renders the contact page."""
    assert isinstance(request, HttpRequest)
    tparams = {
        'title':'Contact',
        'message':'Your contact page.',
        'year':datetime.now().year,
    }
    return render(request, 'contact.html', tparams)


def about(request):
    assert isinstance(request, HttpRequest)
    tparams = {
        'title': 'About',
        'message': 'Your application description page.',
        'year': datetime.now().year,
    }
    return render(
        request,
        'about.html',
        {
            'title': 'About',
            'message': 'Your application description page.',
            'year': datetime.now().year,
        }
    )

